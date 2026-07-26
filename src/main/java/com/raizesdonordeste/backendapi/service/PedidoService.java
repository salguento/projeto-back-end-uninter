package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.CupomAplicadoDTO;
import com.raizesdonordeste.backendapi.dto.ItemPedidoResponseDTO;
import com.raizesdonordeste.backendapi.dto.PedidoDTO;
import com.raizesdonordeste.backendapi.dto.PedidoResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.*;
import com.raizesdonordeste.backendapi.repository.*;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

	@Value("${app.pedido.reserva-minutos:15}")
	private long minutosReserva = 15;

	private static final BigDecimal VALOR_POR_PONTO = new BigDecimal("0.10");
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final Pattern PADRAO_CHAVE_IDEMPOTENCIA = Pattern.compile("[A-Za-z0-9._:-]{8,100}");

	private final PedidoRepository pedidoRepository;
	private final UsuarioRepository usuarioRepository;
	private final UnidadeRepository unidadeRepository;
	private final ProdutoRepository produtoRepository;
	private final EstoqueRepository estoqueRepository;
	private final AcessoUnidadeService acessoUnidadeService;
	private final DescontoService descontoService;
	private final EstoqueService estoqueService;
	private final AutorizacaoPedidoService autorizacaoPedidoService;
	private final ReservaPedidoService reservaPedidoService;
	private final PagamentoService pagamentoService;
	private final FidelidadeService fidelidadeService;

	@Transactional
	public PedidoResponseDTO criarPedido(PedidoDTO dto, String emailUsuario) {
		return criarPedido(dto, emailUsuario, null);
	}

	@Transactional
	public PedidoResponseDTO criarPedido(PedidoDTO dto, String emailUsuario, String chaveIdempotencia) {
		validarDadosEntrada(dto, emailUsuario);
		String chaveNormalizada = normalizarChaveIdempotencia(chaveIdempotencia);

		boolean usarPontos = Boolean.TRUE.equals(dto.getUsarPontos());
		Usuario usuarioAutenticado = buscarUsuarioParaCriacao(
				emailUsuario, chaveNormalizada != null || usarPontos);
		String hashRequisicao = chaveNormalizada != null ? calcularHashRequisicao(dto) : null;
		PedidoResponseDTO respostaAnterior = buscarRespostaIdempotente(
				usuarioAutenticado, chaveNormalizada, hashRequisicao);
		if (respostaAnterior != null) {
			return respostaAnterior;
		}

		Unidade unidade = buscarUnidade(dto.getUnidadeId());
		acessoUnidadeService.verificarAcessoUnidade(dto.getUnidadeId());

		Usuario cliente = determinarCliente(dto, usuarioAutenticado, usarPontos);
		validarPermissaoCriacaoPedido(dto, usuarioAutenticado, cliente);
		if (Boolean.TRUE.equals(dto.getUsarPontos())) {
			fidelidadeService.validarConsentimentoParaResgate(cliente);
		}

		Pedido pedido = inicializarPedido(dto, cliente, usuarioAutenticado, unidade);
		pedido.setChaveIdempotencia(chaveNormalizada);
		pedido.setHashRequisicao(hashRequisicao);
		BigDecimal subtotal = processarItens(dto, pedido, unidade);
		BigDecimal descontoCupom = ZERO;
		CupomAplicadoDTO cupomAplicado = null;

		if (temCodigoCupom(dto)) {
			descontoCupom = descontoService.validarCupom(dto.getCodigoCupom(), subtotal, unidade.getId());
			cupomAplicado = new CupomAplicadoDTO(dto.getCodigoCupom().toUpperCase(), descontoCupom, "CUPOM");
		}

		DescontoFidelidade descontoFidelidade = calcularDescontoFidelidade(dto, cliente, subtotal, descontoCupom);
		BigDecimal descontoTotal = descontoCupom.add(descontoFidelidade.valor());
		BigDecimal valorFinal = subtotal.subtract(descontoTotal);

		validarValorFinal(valorFinal, subtotal, descontoTotal);

		pedido.setDesconto(descontoTotal);
		pedido.setPontosUtilizados(descontoFidelidade.pontosUtilizados());
		pedido.setValorTotal(valorFinal);

		Pedido pedidoSalvo = pedidoRepository.save(pedido);

		if (cupomAplicado != null) {
			descontoService.registrarUsoCupom(dto.getCodigoCupom().toUpperCase(), pedidoSalvo, cliente, descontoCupom);
		}

		log.info("Pedido {} criado | ClienteId: {} | Total: {}", pedidoSalvo.getId(), cliente.getId(), valorFinal);

		PedidoResponseDTO response = converterParaResponseDTO(pedidoSalvo);
		response.setCupomAplicado(cupomAplicado);
		return response;
	}

	private String normalizarChaveIdempotencia(String chaveIdempotencia) {
		if (chaveIdempotencia == null) {
			return null;
		}
		String chave = chaveIdempotencia.trim();
		if (!PADRAO_CHAVE_IDEMPOTENCIA.matcher(chave).matches()) {
			throw new IllegalArgumentException(
					"Idempotency-Key deve conter de 8 a 100 caracteres alfanumericos ou . _ : -. ");
		}
		return chave;
	}

	private Usuario buscarUsuarioParaCriacao(String email, boolean bloquearParaIdempotencia) {
		Optional<Usuario> usuario = bloquearParaIdempotencia
				? usuarioRepository.findByEmailForUpdate(email)
				: usuarioRepository.findByEmail(email);
		return usuario.orElseThrow(
				() -> new RecursoNaoEncontradoException("USUARIO_NAO_ENCONTRADO", "Usuario nao encontrado."));
	}

	private PedidoResponseDTO buscarRespostaIdempotente(Usuario usuario, String chave, String hashRequisicao) {
		if (chave == null) {
			return null;
		}
		Optional<Pedido> pedidoExistente = pedidoRepository
				.findByUsuarioRegistroIdAndChaveIdempotencia(usuario.getId(), chave);
		if (pedidoExistente.isEmpty()) {
			return null;
		}

		Pedido pedido = pedidoExistente.get();
		if (!Objects.equals(hashRequisicao, pedido.getHashRequisicao())) {
			throw new RegraNegocioException("CHAVE_IDEMPOTENCIA_REUTILIZADA",
					"A Idempotency-Key informada ja foi utilizada com outro conteudo.");
		}
		log.info("Requisicao idempotente repetida | UsuarioId: {} | Pedido: {} | Chave: {}",
				usuario.getId(), pedido.getId(), chave);
		return converterParaResponseDTO(pedido);
	}

	private String calcularHashRequisicao(PedidoDTO dto) {
		StringBuilder conteudo = new StringBuilder()
				.append(dto.getUnidadeId()).append('|')
				.append(dto.getCanalPedido()).append('|')
				.append(dto.getFormaPagamento()).append('|')
				.append(Boolean.TRUE.equals(dto.getPedidoInterno())).append('|')
				.append(dto.getClienteId()).append('|')
				.append(Boolean.TRUE.equals(dto.getUsarPontos())).append('|')
				.append(dto.getCodigoCupom() == null ? ""
						: dto.getCodigoCupom().trim().toUpperCase(Locale.ROOT));
		consolidarQuantidades(dto).forEach((produtoId, quantidade) -> conteudo
				.append('|').append(produtoId).append(':').append(quantidade));

		try {
			byte[] hash = MessageDigest.getInstance("SHA-256")
					.digest(conteudo.toString().getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Algoritmo SHA-256 indisponivel.", e);
		}
	}

	@Transactional(readOnly = true)
	public PedidoResponseDTO buscarPorId(Long id, String emailUsuario) {
		Objects.requireNonNull(id, "ID do pedido nao pode ser nulo");
		Objects.requireNonNull(emailUsuario, "Email do usuario nao pode ser nulo");
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("PEDIDO_NAO_ENCONTRADO",
						"Pedido ID " + id + " nao encontrado."));
		autorizacaoPedidoService.verificarConsulta(pedido, emailUsuario);
		return converterParaResponseDTO(pedido);
	}

	@Transactional
	public PedidoResponseDTO atualizarStatus(Long id, StatusPedido novoStatus, String emailUsuario) {
		Objects.requireNonNull(id, "ID do pedido nao pode ser nulo");
		Objects.requireNonNull(novoStatus, "Novo status nao pode ser nulo");

		Pedido pedido = pedidoRepository.findById(id).orElseThrow(
				() -> new RecursoNaoEncontradoException("PEDIDO_NAO_ENCONTRADO", "Pedido nao encontrado."));

		autorizacaoPedidoService.verificarOperacao(pedido, emailUsuario);
		if (novoStatus == StatusPedido.CANCELADO) {
			throw new RegraNegocioException("CANCELAMENTO_REQUER_ROTA_DEDICADA",
					"O cancelamento deve ser solicitado pela rota POST /pedidos/{id}/cancelamento.");
		}
		validarTransicaoStatus(pedido.getStatus(), novoStatus);

		StatusPedido statusAnterior = pedido.getStatus();
		pedido.setStatus(novoStatus);
		Pedido pedidoAtualizado = pedidoRepository.save(pedido);

		log.info("Pedido {} status alterado de {} para {} | Ator: {}",
				id, statusAnterior, novoStatus, LogPseudonymizer.id(emailUsuario));

		if (novoStatus == StatusPedido.ENTREGUE && statusAnterior != StatusPedido.ENTREGUE) {
			atribuirPontosFidelidade(pedidoAtualizado);
		}

		return converterParaResponseDTO(pedidoAtualizado);
	}

	@Transactional
	public PedidoResponseDTO cancelarPedido(Long id, String emailUsuario) {
		Objects.requireNonNull(id, "ID do pedido nao pode ser nulo");
		Objects.requireNonNull(emailUsuario, "Email do usuario nao pode ser nulo");
		Pedido pedido = pedidoRepository.findByIdForUpdate(id).orElseThrow(
				() -> new RecursoNaoEncontradoException("PEDIDO_NAO_ENCONTRADO", "Pedido nao encontrado."));

		autorizacaoPedidoService.verificarCancelamento(pedido, emailUsuario);
		return executarCancelamento(pedido, "CANCELADO_PELO_USUARIO");
	}

	@Transactional(readOnly = true)
	public Page<PedidoResponseDTO> listarFiltrado(CanalPedido canalPedido, Long unidadeId,
			Pageable pageable, String emailUsuario) {
		return listarFiltrado(canalPedido, null, unidadeId, pageable, emailUsuario);
	}

	@Transactional(readOnly = true)
	public Page<PedidoResponseDTO> listarFiltrado(CanalPedido canalPedido, StatusPedido status,
			Long unidadeId, Pageable pageable, String emailUsuario) {
		Objects.requireNonNull(pageable, "Pageable nao pode ser nulo");
		AutorizacaoPedidoService.EscopoAcesso escopo = autorizacaoPedidoService.obterEscopo(emailUsuario);

		Page<Pedido> pedidos;
		if (escopo.isCliente()) {
			pedidos = listarPedidosDoCliente(escopo.usuarioId(), canalPedido, status, unidadeId, pageable);
		} else if (escopo.isAdmin()) {
			pedidos = listarPedidosGlobais(canalPedido, status, unidadeId, pageable);
		} else if (escopo.isFuncionario()) {
			pedidos = listarPedidosDasUnidades(escopo.unidadeIds(), canalPedido, status, unidadeId, pageable);
		} else {
			throw new AccessDeniedException("Perfil sem permissao para consultar pedidos.");
		}

		return pedidos.map(this::converterParaResponseDTO);
	}

	private Page<Pedido> listarPedidosDoCliente(Long usuarioId, CanalPedido canalPedido,
			StatusPedido status, Long unidadeId, Pageable pageable) {
		if (status != null) {
			if (canalPedido != null && unidadeId != null) {
				return pedidoRepository.findByUsuarioIdAndCanalPedidoAndStatusAndUnidadeId(
						usuarioId, canalPedido, status, unidadeId, pageable);
			}
			if (canalPedido != null) {
				return pedidoRepository.findByUsuarioIdAndCanalPedidoAndStatus(
						usuarioId, canalPedido, status, pageable);
			}
			if (unidadeId != null) {
				return pedidoRepository.findByUsuarioIdAndStatusAndUnidadeId(
						usuarioId, status, unidadeId, pageable);
			}
			return pedidoRepository.findByUsuarioIdAndStatus(usuarioId, status, pageable);
		}
		if (canalPedido != null && unidadeId != null) {
			return pedidoRepository.findByUsuarioIdAndCanalPedidoAndUnidadeId(
					usuarioId, canalPedido, unidadeId, pageable);
		}
		if (canalPedido != null) {
			return pedidoRepository.findByUsuarioIdAndCanalPedido(usuarioId, canalPedido, pageable);
		}
		if (unidadeId != null) {
			return pedidoRepository.findByUsuarioIdAndUnidadeId(usuarioId, unidadeId, pageable);
		}
		return pedidoRepository.findByUsuarioId(usuarioId, pageable);
	}

	private Page<Pedido> listarPedidosGlobais(CanalPedido canalPedido, StatusPedido status,
			Long unidadeId, Pageable pageable) {
		if (status != null) {
			if (canalPedido != null && unidadeId != null) {
				return pedidoRepository.findByCanalPedidoAndStatusAndUnidadeId(
						canalPedido, status, unidadeId, pageable);
			}
			if (canalPedido != null) {
				return pedidoRepository.findByCanalPedidoAndStatus(canalPedido, status, pageable);
			}
			if (unidadeId != null) {
				return pedidoRepository.findByStatusAndUnidadeId(status, unidadeId, pageable);
			}
			return pedidoRepository.findByStatus(status, pageable);
		}
		if (canalPedido != null && unidadeId != null) {
			return pedidoRepository.findByCanalPedidoAndUnidadeId(canalPedido, unidadeId, pageable);
		}
		if (canalPedido != null) {
			return pedidoRepository.findByCanalPedido(canalPedido, pageable);
		}
		if (unidadeId != null) {
			return pedidoRepository.findByUnidadeId(unidadeId, pageable);
		}
		return pedidoRepository.findAll(pageable);
	}

	private Page<Pedido> listarPedidosDasUnidades(List<Long> unidadesPermitidas, CanalPedido canalPedido,
			StatusPedido status, Long unidadeId, Pageable pageable) {
		if (unidadeId != null && !unidadesPermitidas.contains(unidadeId)) {
			throw new AccessDeniedException(
					"Voce nao tem permissao para acessar pedidos desta unidade.");
		}
		if (unidadeId != null) {
			if (status != null) {
				return canalPedido == null
						? pedidoRepository.findByStatusAndUnidadeId(status, unidadeId, pageable)
						: pedidoRepository.findByCanalPedidoAndStatusAndUnidadeId(
								canalPedido, status, unidadeId, pageable);
			}
			return canalPedido == null
					? pedidoRepository.findByUnidadeId(unidadeId, pageable)
					: pedidoRepository.findByCanalPedidoAndUnidadeId(canalPedido, unidadeId, pageable);
		}
		if (status != null) {
			return canalPedido == null
					? pedidoRepository.findByStatusAndUnidadeIdIn(status, unidadesPermitidas, pageable)
					: pedidoRepository.findByCanalPedidoAndStatusAndUnidadeIdIn(
							canalPedido, status, unidadesPermitidas, pageable);
		}
		return canalPedido == null
				? pedidoRepository.findByUnidadeIdIn(unidadesPermitidas, pageable)
				: pedidoRepository.findByCanalPedidoAndUnidadeIdIn(canalPedido, unidadesPermitidas, pageable);
	}

	private void validarDadosEntrada(PedidoDTO dto, String emailUsuario) {
		Objects.requireNonNull(dto, "DTO do pedido nao pode ser nulo");
		Objects.requireNonNull(emailUsuario, "Email do usuario nao pode ser nulo");

		if (dto.getItens() == null || dto.getItens().isEmpty()) {
			throw new RegraNegocioException("PEDIDO_INVALIDO", "O pedido deve conter pelo menos um item.");
		}
		if (dto.getUnidadeId() == null) {
			throw new RegraNegocioException("PEDIDO_INVALIDO", "Unidade e obrigatoria.");
		}
		if (dto.getFormaPagamento() == null) {
			throw new RegraNegocioException("PEDIDO_INVALIDO", "Forma de pagamento e obrigatoria.");
		}
		if (dto.getCanalPedido() == null) {
			throw new RegraNegocioException("PEDIDO_INVALIDO", "Canal do pedido e obrigatorio.");
		}
	}

	private Unidade buscarUnidade(Long unidadeId) {
		Unidade unidade = unidadeRepository.findById(unidadeId).orElseThrow(
				() -> new RecursoNaoEncontradoException("UNIDADE_NAO_ENCONTRADA", "Unidade nao encontrada."));
		if (!Boolean.TRUE.equals(unidade.getAtivo())) {
			throw new RegraNegocioException("UNIDADE_INATIVA",
					"A unidade '" + unidade.getNome() + "' esta inativa e nao pode receber pedidos.");
		}
		return unidade;
	}

	private Usuario determinarCliente(PedidoDTO dto, Usuario usuarioAutenticado, boolean bloquearParaPontos) {
		if (dto.getClienteId() != null && !dto.getClienteId().equals(usuarioAutenticado.getId())) {
			Optional<Usuario> cliente = bloquearParaPontos
					? usuarioRepository.findByIdForUpdate(dto.getClienteId())
					: usuarioRepository.findById(dto.getClienteId());
			return cliente.orElseThrow(
					() -> new RecursoNaoEncontradoException("CLIENTE_NAO_ENCONTRADO", "Cliente nao encontrado."));
		}
		return usuarioAutenticado;
	}

	private void validarPermissaoCriacaoPedido(PedidoDTO dto, Usuario usuarioAutenticado, Usuario cliente) {
		boolean pedidoParaOutroCliente = !usuarioAutenticado.getId().equals(cliente.getId());

		if (pedidoParaOutroCliente && usuarioAutenticado.getPerfil() != Perfil.ATENDENTE) {
			throw new AccessDeniedException(
					"Apenas atendentes podem criar pedidos para outros clientes.");
		}

		if (Boolean.TRUE.equals(dto.getPedidoInterno()) && usuarioAutenticado.getPerfil() == Perfil.CLIENTE) {
			throw new AccessDeniedException("Clientes nao podem criar pedidos internos.");
		}
	}

	private Pedido inicializarPedido(PedidoDTO dto, Usuario cliente, Usuario usuarioRegistro, Unidade unidade) {
		Pedido pedido = new Pedido();
		pedido.setUsuario(cliente);
		pedido.setCliente(cliente);
		pedido.setUsuarioRegistro(usuarioRegistro);
		pedido.setUnidade(unidade);
		pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
		pedido.setFormaPagamento(dto.getFormaPagamento());
		pedido.setCanalPedido(dto.getCanalPedido());
		pedido.setCriadoEm(LocalDateTime.now());
		pedido.setExpiraEm(pedido.getCriadoEm().plusMinutes(minutosReserva));
		pedido.setPedidoInterno(Boolean.TRUE.equals(dto.getPedidoInterno()));
		return pedido;
	}

	private BigDecimal processarItens(PedidoDTO dto, Pedido pedido, Unidade unidade) {
		BigDecimal subtotal = ZERO;

		for (Map.Entry<Long, Integer> itemConsolidado : consolidarQuantidades(dto).entrySet()) {
			Long produtoId = itemConsolidado.getKey();
			Integer quantidade = itemConsolidado.getValue();
			Produto produto = produtoRepository.findById(produtoId)
					.orElseThrow(() -> new RecursoNaoEncontradoException("PRODUTO_NAO_ENCONTRADO",
							"Produto ID " + produtoId + " nao encontrado."));

			if (!Boolean.TRUE.equals(produto.getAtivo())) {
				throw new RegraNegocioException("PRODUTO_INATIVO",
						"O produto '" + produto.getNome() + "' nao esta disponivel.");
			}

			reservarEstoque(unidade, produto, quantidade);

			BigDecimal precoUnitario = descontoService.obterPrecoDoProduto(produto, unidade.getId());

			ItemPedido itemPedido = new ItemPedido();
			itemPedido.setProduto(produto);
			itemPedido.setQuantidade(quantidade);
			itemPedido.setPrecoUnitario(precoUnitario);
			pedido.adicionarItem(itemPedido);

			subtotal = subtotal.add(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
		}

		return subtotal;
	}

	private Map<Long, Integer> consolidarQuantidades(PedidoDTO dto) {
		Map<Long, Integer> quantidades = new TreeMap<>();
		for (var item : dto.getItens()) {
			if (item.getProdutoId() == null) {
				throw new RegraNegocioException("PEDIDO_INVALIDO", "Produto e obrigatorio.");
			}
			if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
				throw new RegraNegocioException("QUANTIDADE_INVALIDA", "Quantidade deve ser maior que zero.");
			}
			try {
				quantidades.merge(item.getProdutoId(), item.getQuantidade(), Math::addExact);
			} catch (ArithmeticException e) {
				throw new RegraNegocioException("QUANTIDADE_INVALIDA", "Quantidade total excede o limite permitido.");
			}
		}
		return quantidades;
	}

	private void reservarEstoque(Unidade unidade, Produto produto, Integer quantidade) {
		int linhasAtualizadas = estoqueRepository.reservarEstoqueSeDisponivel(
				unidade.getId(), produto.getId(), quantidade);
		if (linhasAtualizadas == 1) {
			return;
		}

		boolean estoqueExiste = estoqueRepository
				.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()).isPresent();
		if (!estoqueExiste) {
			throw new RecursoNaoEncontradoException("ESTOQUE_NAO_ENCONTRADO",
					"Estoque nao encontrado para o produto '" + produto.getNome() + "'.");
		}
		throw new RegraNegocioException("ESTOQUE_INSUFICIENTE",
				"Estoque insuficiente para: " + produto.getNome());
	}

	private boolean temCodigoCupom(PedidoDTO dto) {
		return dto.getCodigoCupom() != null && !dto.getCodigoCupom().isBlank();
	}

	private DescontoFidelidade calcularDescontoFidelidade(PedidoDTO dto, Usuario cliente, BigDecimal subtotal,
			BigDecimal descontoCupom) {
		if (!Boolean.TRUE.equals(dto.getUsarPontos())) {
			return DescontoFidelidade.semDesconto();
		}

		int pontos = cliente.getPontos() != null ? cliente.getPontos() : 0;
		if (pontos <= 0) {
			return DescontoFidelidade.semDesconto();
		}

		BigDecimal subtotalAposCupom = subtotal.subtract(descontoCupom);
		BigDecimal descontoMaximo = BigDecimal.valueOf(pontos).multiply(VALOR_POR_PONTO);

		BigDecimal descontoFidelidade;
		int pontosUsados;

		if (descontoMaximo.compareTo(subtotalAposCupom) >= 0) {
			descontoFidelidade = subtotalAposCupom;
			pontosUsados = subtotalAposCupom.divide(VALOR_POR_PONTO, 0, RoundingMode.CEILING).intValue();
		} else {
			descontoFidelidade = descontoMaximo;
			pontosUsados = pontos;
		}

		cliente.setPontos(pontos - pontosUsados);
		usuarioRepository.save(cliente);

		return new DescontoFidelidade(descontoFidelidade, pontosUsados);
	}

	private void validarValorFinal(BigDecimal valorFinal, BigDecimal subtotal, BigDecimal descontoTotal) {
		if (valorFinal.compareTo(ZERO) < 0) {
			log.error("Valor final negativo | Subtotal: {} | Descontos: {}", subtotal, descontoTotal);
			throw new RegraNegocioException("VALOR_INVALIDO", "O valor total do pedido nao pode ser negativo.");
		}
	}

	private void validarTransicaoStatus(StatusPedido statusAtual, StatusPedido novoStatus) {
		if (statusAtual == StatusPedido.AGUARDANDO_PAGAMENTO) {
			throw new RegraNegocioException("PAGAMENTO_PENDENTE",
					"Nao e permitido alterar o status sem confirmacao de pagamento.");
		}
		if (statusAtual == StatusPedido.ENTREGUE || statusAtual == StatusPedido.CANCELADO) {
			throw new RegraNegocioException("PEDIDO_FINALIZADO",
					"Nao e possivel alterar o status de um pedido finalizado.");
		}

		StatusPedido proximoStatus = switch (statusAtual) {
			case RECEBIDO -> StatusPedido.EM_PREPARACAO;
			case EM_PREPARACAO -> StatusPedido.PRONTO;
			case PRONTO -> StatusPedido.ENTREGUE;
			default -> null;
		};

		if (novoStatus != proximoStatus) {
			throw new RegraNegocioException("TRANSICAO_STATUS_INVALIDA",
					"Transicao de " + statusAtual + " para " + novoStatus
							+ " nao permitida. Proximo status esperado: " + proximoStatus + ".");
		}
	}

	private void atribuirPontosFidelidade(Pedido pedido) {
		if (pedido.getValorTotal() == null || pedido.getUsuario() == null) {
			return;
		}

		int pontosGanhos = pedido.getValorTotal().intValue();
		if (pontosGanhos <= 0) {
			return;
		}

		Usuario cliente = pedido.getUsuario();
		int pontosAtuais = cliente.getPontos() != null ? cliente.getPontos() : 0;
		cliente.setPontos(pontosAtuais + pontosGanhos);
		pedido.setPontosConcedidos(pontosGanhos);
		usuarioRepository.save(cliente);

		log.info("ClienteId {} recebeu {} pontos | Novo saldo: {}", cliente.getId(), pontosGanhos,
				cliente.getPontos());
	}


	private PedidoResponseDTO converterParaResponseDTO(Pedido pedido) {
	    PedidoResponseDTO response = new PedidoResponseDTO();
	    response.setPedidoId(pedido.getId());
	    response.setStatus(pedido.getStatus());
	    response.setCanalPedido(pedido.getCanalPedido());
	    response.setTotal(pedido.getValorTotal());
	    response.setDesconto(pedido.getDesconto());
	    response.setPontosUtilizados(pedido.getPontosUtilizados());
	    response.setPontosConcedidos(pedido.getPontosConcedidos());
	    response.setFormaPagamento(pedido.getFormaPagamento());
	    response.setDataCriacao(pedido.getCriadoEm());
	    response.setExpiraEm(pedido.getExpiraEm());
	    response.setPedidoInterno(pedido.getPedidoInterno());
	    
	    if (pedido.getUsuarioRegistro() != null) {
	        response.setUsuarioRegistroId(pedido.getUsuarioRegistro().getId());
	    }
	    if (pedido.getUsuario() != null) {
	        response.setClienteId(pedido.getUsuario().getId());
	    }
	    
	    List<ItemPedidoResponseDTO> itensDto = new ArrayList<>();
	    if (pedido.getItens() != null) {
	        for (ItemPedido item : pedido.getItens()) {
	            ItemPedidoResponseDTO itemDto = new ItemPedidoResponseDTO();
	            itemDto.setProdutoId(item.getProduto().getId());
	            itemDto.setNomeProduto(item.getProduto().getNome());
	            itemDto.setQuantidade(item.getQuantidade());
	            itemDto.setPrecoUnitario(item.getPrecoUnitario());
	            itensDto.add(itemDto);
	        }
	    }
	    response.setItens(itensDto);
	    
	    return response;
	}

	private PedidoResponseDTO executarCancelamento(Pedido pedido, String motivo) {
		StatusPedido statusAnterior = pedido.getStatus();
		if (statusAnterior == StatusPedido.CANCELADO) {
			throw new RegraNegocioException("PEDIDO_FINALIZADO", "O pedido ja esta cancelado.");
		}

		if (statusAnterior == StatusPedido.AGUARDANDO_PAGAMENTO) {
			reservaPedidoService.cancelarEDevolverEstoque(pedido, motivo);
			return converterParaResponseDTO(pedido);
		}

		pagamentoService.estornarPagamento(pedido);
		pedido.setStatus(StatusPedido.CANCELADO);

		if (statusAnterior == StatusPedido.RECEBIDO) {
			devolverEstoqueDoPedido(pedido);
		}

		descontoService.estornarBeneficios(pedido);
		pedidoRepository.save(pedido);
		log.info("Pedido {} cancelado com estorno financeiro | Status anterior: {} | Motivo: {}",
				pedido.getId(), statusAnterior, motivo);
		return converterParaResponseDTO(pedido);
	}

	private void devolverEstoqueDoPedido(Pedido pedido) {
		if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
			return;
		}

		for (ItemPedido item : pedido.getItens()) {
			try {
				estoqueService.estornarEstoque(item.getProduto().getId(), pedido.getUnidade().getId(),
						item.getQuantidade());
			} catch (RecursoNaoEncontradoException e) {
				log.error("Falha ao devolver estoque | Pedido: {} | Produto: {} | Erro: {}", pedido.getId(),
						item.getProduto().getId(), e.getMessage());
				throw new RegraNegocioException("ESTORNO_FALHOU",
						"Falha ao devolver estoque do produto ID " + item.getProduto().getId());
			}
		}

		log.info("Estoque devolvido | Pedido: {} | Total itens: {}", pedido.getId(), pedido.getItens().size());
	}

	private record DescontoFidelidade(BigDecimal valor, int pontosUtilizados) {
		private static DescontoFidelidade semDesconto() {
			return new DescontoFidelidade(BigDecimal.ZERO, 0);
		}
	}
}

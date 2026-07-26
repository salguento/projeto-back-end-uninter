package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.PagamentoRequestDTO;
import com.raizesdonordeste.backendapi.dto.PagamentoResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.exception.ReservaExpiradaException;
import com.raizesdonordeste.backendapi.gateway.GatewayPagamento;
import com.raizesdonordeste.backendapi.gateway.ResultadoGatewayEstorno;
import com.raizesdonordeste.backendapi.model.*;
import com.raizesdonordeste.backendapi.repository.PagamentoRepository;
import com.raizesdonordeste.backendapi.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private static final String STATUS_APROVADO = "APROVADO";
    private static final String STATUS_RECUSADO = "RECUSADO";
    private static final String STATUS_ESTORNADO = "ESTORNADO";
    private static final String PREFIXO_TRANSACAO = "TXN-";

    private static final String PEDIDO_NAO_ENCONTRADO = "PEDIDO_NAO_ENCONTRADO";
    private static final String PEDIDO_INVALIDO = "PEDIDO_INVALIDO";
    private static final String PAGAMENTO_JA_PROCESSADO = "PAGAMENTO_JA_PROCESSADO";
    private static final String VALOR_INVALIDO = "VALOR_INVALIDO";

    @org.springframework.beans.factory.annotation.Value("${app.pagamento.max-tentativas:3}")
    private int maxTentativas = 3;

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AutorizacaoPedidoService autorizacaoPedidoService;
    private final ReservaPedidoService reservaPedidoService;
    private final GatewayPagamento gatewayPagamento;

    @Transactional(noRollbackFor = ReservaExpiradaException.class)
    public PagamentoResponseDTO processarPagamento(Long pedidoId, PagamentoRequestDTO dto, String emailUsuario) {
        Objects.requireNonNull(pedidoId, "PedidoId nao pode ser nulo");
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        Objects.requireNonNull(dto.getFormaPagamento(), "Forma de pagamento e obrigatoria");
        Objects.requireNonNull(emailUsuario, "Email do usuario nao pode ser nulo");
        
        Pedido pedido = buscarPedidoParaPagamento(pedidoId);
        autorizacaoPedidoService.verificarPagamento(pedido, emailUsuario);
        
        validarReservaVigente(pedido);
        validarPagamentoAindaNaoAprovado(pedido);
        validarStatusPedido(pedido);
        long tentativasAnteriores = pagamentoRepository.countByPedido(pedido);
        validarLimiteTentativas(tentativasAnteriores);
        validarValorPedido(pedido.getValorTotal());
        
        String statusPagamento = gatewayPagamento.processar(dto.getFormaPagamento()).name();
        String codigoTransacao = gerarCodigoTransacao();
        
        Pagamento pagamento = criarPagamento(pedido, dto.getFormaPagamento(), statusPagamento, codigoTransacao);
        Pagamento pagamentoSalvo = pagamentoRepository.save(pagamento);
        
        atualizarStatusPedido(pedido, statusPagamento, codigoTransacao, tentativasAnteriores + 1);
        pedidoRepository.save(pedido);
        
        log.info("Pagamento processado | Pedido: {} | Status: {} | Codigo: {}",
                pedidoId, statusPagamento, codigoTransacao);
        
        return converterParaResponseDTO(pagamentoSalvo, tentativasAnteriores + 1);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponseDTO> listarTentativas(Long pedidoId, String emailUsuario) {
        Objects.requireNonNull(pedidoId, "PedidoId nao pode ser nulo");
        Objects.requireNonNull(emailUsuario, "Email do usuario nao pode ser nulo");
        Pedido pedido = buscarPedido(pedidoId);
        autorizacaoPedidoService.verificarConsulta(pedido, emailUsuario);

        List<Pagamento> pagamentos = pagamentoRepository.findAllByPedidoOrderByCriadoEmAsc(pedido);
        int total = pagamentos.size();
        return java.util.stream.IntStream.range(0, total)
                .mapToObj(i -> converterParaResponseDTO(pagamentos.get(i), i + 1L))
                .toList();
    }

    @Transactional
    public PagamentoResponseDTO estornarPagamento(Pedido pedido) {
        Objects.requireNonNull(pedido, "Pedido nao pode ser nulo");

        Pagamento pagamento = pagamentoRepository
                .findFirstByPedidoAndStatusAndEstornadoEmIsNullOrderByCriadoEmDesc(pedido, STATUS_APROVADO)
                .orElseThrow(() -> new RegraNegocioException("PAGAMENTO_NAO_ESTORNAVEL",
                        "O pedido nao possui pagamento aprovado disponivel para estorno."));

        ResultadoGatewayEstorno resultadoEstorno = gatewayPagamento.estornar(pagamento.getCodigoTransacao());
        pagamento.setCodigoEstorno(resultadoEstorno.codigoConfirmacao());
        pagamento.setEstornadoEm(LocalDateTime.now());
        Pagamento pagamentoEstornado = pagamentoRepository.save(pagamento);

        log.info("Pagamento estornado | Pedido: {} | Transacao: {} | Estorno: {}",
                pedido.getId(), pagamento.getCodigoTransacao(), pagamento.getCodigoEstorno());
        return converterParaResponseDTO(pagamentoEstornado, pagamentoRepository.countByPedido(pedido));
    }

    private Pedido buscarPedido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(PEDIDO_NAO_ENCONTRADO,
                        "Pedido ID " + pedidoId + " nao encontrado."));
    }

    private Pedido buscarPedidoParaPagamento(Long pedidoId) {
        Optional<Pedido> pedidoBloqueado = pedidoRepository.findByIdForUpdate(pedidoId);
        // O fallback facilita doubles de teste antigos; no banco real a consulta bloqueada retorna o pedido.
        return pedidoBloqueado.or(() -> pedidoRepository.findById(pedidoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException(PEDIDO_NAO_ENCONTRADO,
                        "Pedido ID " + pedidoId + " nao encontrado."));
    }

    private void validarStatusPedido(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException(PEDIDO_INVALIDO,
                    "Pedido nao esta aguardando pagamento. Status atual: " + pedido.getStatus());
        }
    }

    private void validarPagamentoAindaNaoAprovado(Pedido pedido) {
        if (pagamentoRepository.existsByPedidoAndStatusAndEstornadoEmIsNull(pedido, STATUS_APROVADO)) {
            throw new RegraNegocioException(PAGAMENTO_JA_PROCESSADO,
                    "Este pedido ja possui um pagamento aprovado.");
        }
    }

    private void validarReservaVigente(Pedido pedido) {
        if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO
                && pedido.getExpiraEm() != null
                && !LocalDateTime.now().isBefore(pedido.getExpiraEm())) {
            reservaPedidoService.cancelarEDevolverEstoque(pedido, "PRAZO_EXPIRADO");
            throw new ReservaExpiradaException("A reserva de estoque expirou e o pedido foi cancelado.");
        }
    }

    private void validarLimiteTentativas(long tentativasAnteriores) {
        if (tentativasAnteriores >= maxTentativas) {
            throw new RegraNegocioException("LIMITE_TENTATIVAS_ATINGIDO",
                    "O limite de tentativas de pagamento foi atingido.");
        }
    }

    private void validarValorPedido(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException(VALOR_INVALIDO,
                    "O valor do pedido deve ser maior que zero.");
        }
    }

    private String gerarCodigoTransacao() {
        return PREFIXO_TRANSACAO + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Pagamento criarPagamento(Pedido pedido, FormaPagamento formaPagamento,
                                      String statusPagamento, String codigoTransacao) {
        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setFormaPagamento(formaPagamento);
        pagamento.setValor(pedido.getValorTotal());
        pagamento.setStatus(statusPagamento);
        pagamento.setCodigoTransacao(codigoTransacao);
        return pagamento;
    }

    private void atualizarStatusPedido(Pedido pedido, String statusPagamento, String codigoTransacao,
                                       long numeroTentativa) {
        if (STATUS_APROVADO.equals(statusPagamento)) {
            pedido.setStatus(StatusPedido.RECEBIDO);
            log.info("Auditoria: Pagamento APROVADO para pedido {}. Codigo: {}",
                    pedido.getId(), codigoTransacao);
        } else {
            log.warn("Auditoria: Pagamento RECUSADO para pedido {}. Codigo: {}",
                    pedido.getId(), codigoTransacao);
            if (numeroTentativa >= maxTentativas) {
                reservaPedidoService.cancelarEDevolverEstoque(pedido, "LIMITE_TENTATIVAS");
            }
        }
    }

    private PagamentoResponseDTO converterParaResponseDTO(Pagamento pagamento, long numeroTentativa) {
        PagamentoResponseDTO response = new PagamentoResponseDTO();
        response.setPagamentoId(pagamento.getId());
        response.setPedidoId(pagamento.getPedido().getId());
        response.setFormaPagamento(pagamento.getFormaPagamento());
        response.setValor(pagamento.getValor());
        response.setStatus(pagamento.getEstornadoEm() != null ? STATUS_ESTORNADO : pagamento.getStatus());
        response.setCodigoTransacao(pagamento.getCodigoTransacao());
        response.setCodigoEstorno(pagamento.getCodigoEstorno());
        response.setEstornadoEm(pagamento.getEstornadoEm());
        response.setCriadoEm(pagamento.getCriadoEm());
        response.setStatusPedido(pagamento.getPedido().getStatus());
        response.setTentativasRestantes(Math.max(0, maxTentativas - (int) numeroTentativa));
        return response;
    }
}

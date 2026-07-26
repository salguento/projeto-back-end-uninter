package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.ProdutoEstoqueDTO;
import com.raizesdonordeste.backendapi.dto.ProdutoDTO;
import com.raizesdonordeste.backendapi.dto.ProdutoResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Estoque;
import com.raizesdonordeste.backendapi.model.Produto;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.repository.EstoqueRepository;
import com.raizesdonordeste.backendapi.repository.ProdutoRepository;
import com.raizesdonordeste.backendapi.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

	private static final String PRECO_INVALIDO = "PRECO_INVALIDO";
	private static final String NOME_INVALIDO = "NOME_INVALIDO";
	private static final String UNIDADE_NAO_ENCONTRADA = "UNIDADE_NAO_ENCONTRADA";
	private static final String UNIDADE_INATIVA = "UNIDADE_INATIVA";

	private final ProdutoRepository produtoRepository;
	private final EstoqueRepository estoqueRepository;
	private final UnidadeRepository unidadeRepository;
	private final AcessoUnidadeService acessoUnidadeService;

	@Transactional
	public ProdutoResponseDTO criar(ProdutoDTO dto) {
		validarDadosEntrada(dto);
		validarNome(dto.getNome());
		validarPreco(dto.getPrecoVigente());

		Produto produto = new Produto();
		produto.setNome(dto.getNome().trim());
		produto.setDescricao(dto.getDescricao());
		produto.setPrecoVigente(dto.getPrecoVigente());
		produto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : Boolean.TRUE);

		Produto produtoSalvo = produtoRepository.save(produto);

		log.info("Produto {} criado | Nome: {} | Preco: {}", produtoSalvo.getId(), produtoSalvo.getNome(),
				produtoSalvo.getPrecoVigente());

		return converterParaResponseDTO(produtoSalvo);
	}

	@Transactional(readOnly = true)
	public Page<ProdutoResponseDTO> listarAtivos(Pageable pageable) {
		Objects.requireNonNull(pageable, "Pageable nao pode ser nulo");
		return produtoRepository.findByAtivoTrue(pageable).map(this::converterParaResponseDTO);
	}

	@Transactional(readOnly = true)
	public ProdutoResponseDTO buscarPorId(Long id) {
		Objects.requireNonNull(id, "ID do produto nao pode ser nulo");
		Produto produto = produtoRepository.findByIdAndAtivoTrue(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("PRODUTO_NAO_ENCONTRADO",
						"Produto ID " + id + " nao encontrado."));
		return converterParaResponseDTO(produto);
	}

	@Transactional(readOnly = true)
	public Page<ProdutoEstoqueDTO> listarComEstoqueNaUnidade(Long unidadeId, Pageable pageable) {
		Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");
		Objects.requireNonNull(pageable, "Pageable nao pode ser nulo");
		acessoUnidadeService.verificarAcessoUnidade(unidadeId);
		validarUnidadeAtiva(unidadeId);

		Page<Produto> produtos = produtoRepository.findByAtivoTrue(pageable);
		Map<Long, Integer> estoqueMap = construirMapaEstoque(unidadeId);

		Page<ProdutoEstoqueDTO> resultado = produtos
				.map(produto -> converterParaProdutoEstoqueDTO(produto, estoqueMap));

		log.info("Produtos com estoque da unidade {} | Total: {}", unidadeId, resultado.getTotalElements());

		return resultado;
	}

	private void validarUnidadeAtiva(Long unidadeId) {
		Unidade unidade = unidadeRepository.findById(unidadeId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(UNIDADE_NAO_ENCONTRADA,
						"Unidade ID " + unidadeId + " nao encontrada."));

		if (!Boolean.TRUE.equals(unidade.getAtivo())) {
			throw new RegraNegocioException(UNIDADE_INATIVA,
					"A unidade '" + unidade.getNome() + "' esta inativa e nao pode disponibilizar produtos.");
		}
	}

	@Transactional
	public ProdutoResponseDTO atualizar(Long id, ProdutoDTO dto) {
		Objects.requireNonNull(id, "ID do produto nao pode ser nulo");
		Objects.requireNonNull(dto, "DTO nao pode ser nulo");
		validarNome(dto.getNome());
		validarPreco(dto.getPrecoVigente());

		Produto produto = buscarProduto(id);

		produto.setNome(dto.getNome().trim());
		produto.setDescricao(dto.getDescricao());
		produto.setPrecoVigente(dto.getPrecoVigente());

		if (dto.getAtivo() != null) {
			produto.setAtivo(dto.getAtivo());
		}

		Produto produtoAtualizado = produtoRepository.save(produto);

		log.info("Produto {} atualizado | Nome: {} | Preco: {}", id, produtoAtualizado.getNome(),
				produtoAtualizado.getPrecoVigente());

		return converterParaResponseDTO(produtoAtualizado);
	}

	@Transactional
	public void desativar(Long id) {
		Objects.requireNonNull(id, "ID do produto nao pode ser nulo");
		Produto produto = buscarProduto(id);

		if (!Boolean.TRUE.equals(produto.getAtivo())) {
			log.warn("Produto {} ja esta inativo", id);
			return;
		}

		produto.setAtivo(Boolean.FALSE);
		produtoRepository.save(produto);

		log.info("Produto {} desativado (soft-delete)", id);
	}

	private void validarDadosEntrada(ProdutoDTO dto) {
		Objects.requireNonNull(dto, "DTO nao pode ser nulo");
	}

	private void validarNome(String nome) {
		if (nome == null || nome.trim().isEmpty()) {
			throw new RegraNegocioException(NOME_INVALIDO, "O nome do produto e obrigatorio.");
		}
		if (nome.trim().length() < 3) {
			throw new RegraNegocioException(NOME_INVALIDO, "O nome do produto deve ter no minimo 3 caracteres.");
		}
	}

	private void validarPreco(BigDecimal preco) {
		if (preco == null) {
			throw new RegraNegocioException(PRECO_INVALIDO, "O preco do produto e obrigatorio.");
		}
		if (preco.compareTo(BigDecimal.ZERO) <= 0) {
			throw new RegraNegocioException(PRECO_INVALIDO, "O preco do produto deve ser maior que zero.");
		}
	}

	private Produto buscarProduto(Long id) {
		return produtoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("PRODUTO_NAO_ENCONTRADO",
						"Produto ID " + id + " nao encontrado"));
	}

	private Map<Long, Integer> construirMapaEstoque(Long unidadeId) {
		List<Estoque> estoques = estoqueRepository.findByUnidadeId(unidadeId);
		return estoques.stream()
				.collect(Collectors.toMap(e -> e.getProduto().getId(), Estoque::getQuantidade, (v1, v2) -> v1));
	}

	private ProdutoEstoqueDTO converterParaProdutoEstoqueDTO(Produto produto, Map<Long, Integer> estoqueMap) {
		Integer quantidade = estoqueMap.getOrDefault(produto.getId(), 0);
		return new ProdutoEstoqueDTO(produto.getId(), produto.getNome(), produto.getDescricao(),
				produto.getPrecoVigente(), quantidade, produto.getAtivo());
	}

	private ProdutoResponseDTO converterParaResponseDTO(Produto produto) {
		ProdutoResponseDTO response = new ProdutoResponseDTO();
		response.setId(produto.getId());
		response.setNome(produto.getNome());
		response.setDescricao(produto.getDescricao());
		response.setPrecoVigente(produto.getPrecoVigente());
		response.setAtivo(produto.getAtivo());
		return response;
	}
}

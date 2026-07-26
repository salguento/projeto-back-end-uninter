package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.EstoqueDTO;
import com.raizesdonordeste.backendapi.dto.EstoqueResponseDTO;
import com.raizesdonordeste.backendapi.dto.MovimentacaoEstoqueRequestDTO;
import com.raizesdonordeste.backendapi.dto.MovimentacaoEstoqueResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Estoque;
import com.raizesdonordeste.backendapi.model.Produto;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.model.MovimentacaoEstoque;
import com.raizesdonordeste.backendapi.model.TipoMovimentacaoEstoque;
import com.raizesdonordeste.backendapi.repository.EstoqueRepository;
import com.raizesdonordeste.backendapi.repository.MovimentacaoEstoqueRepository;
import com.raizesdonordeste.backendapi.repository.ProdutoRepository;
import com.raizesdonordeste.backendapi.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private static final String ESTOQUE_NAO_ENCONTRADO = "ESTOQUE_NAO_ENCONTRADO";
    private static final String ESTOQUE_JA_EXISTE = "ESTOQUE_JA_EXISTE";
    private static final String PRODUTO_NAO_ENCONTRADO = "PRODUTO_NAO_ENCONTRADO";
    private static final String UNIDADE_NAO_ENCONTRADA = "UNIDADE_NAO_ENCONTRADA";
    private static final String UNIDADE_INATIVA = "UNIDADE_INATIVA";
    private static final String QUANTIDADE_INVALIDA = "QUANTIDADE_INVALIDA";
    private static final String PRODUTO_INATIVO = "PRODUTO_INATIVO";
    private static final String ESTOQUE_INSUFICIENTE = "ESTOQUE_INSUFICIENTE";

    private final EstoqueRepository estoqueRepository;
    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final AcessoUnidadeService acessoUnidadeService;

    @Transactional
    public EstoqueResponseDTO criar(EstoqueDTO dto) {
        validarDadosEntrada(dto);
        validarQuantidadePositiva(dto.getQuantidade());

        Produto produto = buscarProdutoAtivo(dto.getProdutoId());
        Unidade unidade = buscarUnidade(dto.getUnidadeId());
        validarEstoqueNaoExistente(unidade.getId(), produto.getId());
        acessoUnidadeService.verificarAcessoUnidade(unidade.getId());

        Estoque estoque = new Estoque();
        estoque.setProduto(produto);
        estoque.setUnidade(unidade);
        estoque.setQuantidade(dto.getQuantidade());

        Estoque estoqueSalvo = estoqueRepository.save(estoque);

        log.info("Estoque {} criado | Produto: {} | Unidade: {} | Quantidade: {}",
                estoqueSalvo.getId(), produto.getNome(), unidade.getNome(), dto.getQuantidade());

        return converterParaResponseDTO(estoqueSalvo);
    }

    @Transactional(readOnly = true)
    public Page<EstoqueResponseDTO> listarPorUnidade(Long unidadeId, Pageable pageable) {
        Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");
        acessoUnidadeService.verificarAcessoUnidade(unidadeId);

        Page<Estoque> estoques = estoqueRepository.findByUnidadeId(unidadeId, pageable);
        log.info("Estoque da unidade {} listado | Total: {}", unidadeId, estoques.getTotalElements());

        return estoques.map(this::converterParaResponseDTO);
    }

    @Transactional(readOnly = true)
    public EstoqueResponseDTO buscarPorId(Long id) {
        Objects.requireNonNull(id, "ID do estoque nao pode ser nulo");
        Estoque estoque = buscarEstoque(id);
        acessoUnidadeService.verificarAcessoUnidade(estoque.getUnidade().getId());
        return converterParaResponseDTO(estoque);
    }

    @Transactional
    public EstoqueResponseDTO atualizarQuantidade(Long id, EstoqueDTO dto) {
        Objects.requireNonNull(id, "ID do estoque nao pode ser nulo");
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        validarQuantidadePositiva(dto.getQuantidade());

        Estoque estoque = buscarEstoque(id);
        acessoUnidadeService.verificarAcessoUnidade(estoque.getUnidade().getId());

        Integer quantidadeAnterior = estoque.getQuantidade();
        estoque.setQuantidade(dto.getQuantidade());

        Estoque estoqueAtualizado = estoqueRepository.save(estoque);

        log.info("Estoque {} atualizado | Quantidade: {} -> {}",
                id, quantidadeAnterior, dto.getQuantidade());

        return converterParaResponseDTO(estoqueAtualizado);
    }

    @Transactional
    public void deletar(Long id) {
        Objects.requireNonNull(id, "ID do estoque nao pode ser nulo");
        Estoque estoque = buscarEstoque(id);
        acessoUnidadeService.verificarAcessoUnidade(estoque.getUnidade().getId());

        estoqueRepository.deleteById(id);

        log.info("Estoque {} excluido", id);
    }

    @Transactional
    public MovimentacaoEstoqueResponseDTO movimentar(Long estoqueId, MovimentacaoEstoqueRequestDTO dto,
            String emailUsuario) {
        Objects.requireNonNull(estoqueId, "ID do estoque nao pode ser nulo");
        validarMovimentacao(dto);

        Estoque estoque = estoqueRepository.findByIdForUpdate(estoqueId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ESTOQUE_NAO_ENCONTRADO,
                        "Estoque ID " + estoqueId + " nao encontrado."));
        acessoUnidadeService.verificarAcessoUnidade(estoque.getUnidade().getId());

        int saldoAnterior = estoque.getQuantidade();
        int saldoPosterior = calcularSaldoPosterior(saldoAnterior, dto);
        estoque.setQuantidade(saldoPosterior);
        estoqueRepository.save(estoque);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setEstoque(estoque);
        movimentacao.setTipo(dto.getTipo());
        movimentacao.setQuantidade(dto.getQuantidade());
        movimentacao.setSaldoAnterior(saldoAnterior);
        movimentacao.setSaldoPosterior(saldoPosterior);
        movimentacao.setMotivo(dto.getMotivo().trim());
        movimentacao.setAtor(LogPseudonymizer.id(emailUsuario));
        movimentacao.setCriadoEm(LocalDateTime.now());
        movimentacaoEstoqueRepository.save(movimentacao);

        log.info("Movimentacao de estoque registrada | Estoque: {} | Tipo: {} | Quantidade: {} | Saldo: {} -> {} | Ator: {}",
                estoqueId, dto.getTipo(), dto.getQuantidade(), saldoAnterior, saldoPosterior,
                movimentacao.getAtor());
        return converterMovimentacaoParaResponseDTO(movimentacao);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoqueResponseDTO> listarMovimentacoes(Long estoqueId, Pageable pageable) {
        Objects.requireNonNull(estoqueId, "ID do estoque nao pode ser nulo");
        Objects.requireNonNull(pageable, "Pageable nao pode ser nulo");
        Estoque estoque = buscarEstoque(estoqueId);
        acessoUnidadeService.verificarAcessoUnidade(estoque.getUnidade().getId());
        return movimentacaoEstoqueRepository.findByEstoqueIdOrderByCriadoEmDesc(estoqueId, pageable)
                .map(this::converterMovimentacaoParaResponseDTO);
    }
    
    @Transactional
    public void estornarEstoque(Long produtoId, Long unidadeId, int quantidade) {
        Objects.requireNonNull(produtoId, "ProdutoId nao pode ser nulo");
        Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");
        
        if (quantidade <= 0) {
            throw new RegraNegocioException(QUANTIDADE_INVALIDA,
                    "Quantidade para estorno deve ser maior que zero.");
        }

        Estoque estoque = estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ESTOQUE_NAO_ENCONTRADO,
                        "Estoque nao encontrado para produto ID " + produtoId + " na unidade ID " + unidadeId));

        Integer quantidadeAnterior = estoque.getQuantidade();
        estoque.setQuantidade(quantidadeAnterior + quantidade);
        estoqueRepository.save(estoque);

        log.info("Estoque estornado | Produto: {} | Unidade: {} | Quantidade: {} -> {}",
                produtoId, unidadeId, quantidadeAnterior, estoque.getQuantidade());
    }

    private void validarDadosEntrada(EstoqueDTO dto) {
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        Objects.requireNonNull(dto.getProdutoId(), "ProdutoId nao pode ser nulo");
        Objects.requireNonNull(dto.getUnidadeId(), "UnidadeId nao pode ser nulo");
        Objects.requireNonNull(dto.getQuantidade(), "Quantidade nao pode ser nula");
    }

    private void validarQuantidadePositiva(Integer quantidade) {
        if (quantidade < 0) {
            throw new RegraNegocioException(QUANTIDADE_INVALIDA,
                    "A quantidade nao pode ser negativa.");
        }
    }

    private void validarMovimentacao(MovimentacaoEstoqueRequestDTO dto) {
        Objects.requireNonNull(dto, "DTO da movimentacao nao pode ser nulo");
        Objects.requireNonNull(dto.getTipo(), "Tipo da movimentacao nao pode ser nulo");
        Objects.requireNonNull(dto.getQuantidade(), "Quantidade da movimentacao nao pode ser nula");
        if (dto.getQuantidade() <= 0) {
            throw new RegraNegocioException(QUANTIDADE_INVALIDA,
                    "A quantidade da movimentacao deve ser maior que zero.");
        }
        if (dto.getMotivo() == null || dto.getMotivo().isBlank()) {
            throw new RegraNegocioException("MOTIVO_OBRIGATORIO",
                    "O motivo da movimentacao e obrigatorio.");
        }
    }

    private int calcularSaldoPosterior(int saldoAnterior, MovimentacaoEstoqueRequestDTO dto) {
        if (dto.getTipo() == TipoMovimentacaoEstoque.ENTRADA) {
            return saldoAnterior + dto.getQuantidade();
        }
        if (dto.getQuantidade() > saldoAnterior) {
            throw new RegraNegocioException(ESTOQUE_INSUFICIENTE,
                    "Saldo insuficiente para realizar a saida de estoque.");
        }
        return saldoAnterior - dto.getQuantidade();
    }

    private Produto buscarProdutoAtivo(Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(PRODUTO_NAO_ENCONTRADO,
                        "Produto ID " + produtoId + " nao encontrado."));

        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new RegraNegocioException(PRODUTO_INATIVO,
                    "Produto '" + produto.getNome() + "' esta inativo.");
        }

        return produto;
    }

    private Unidade buscarUnidade(Long unidadeId) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(UNIDADE_NAO_ENCONTRADA,
                        "Unidade ID " + unidadeId + " nao encontrada."));
        if (!Boolean.TRUE.equals(unidade.getAtivo())) {
            throw new RegraNegocioException(UNIDADE_INATIVA,
                    "A unidade '" + unidade.getNome() + "' esta inativa e nao pode receber estoque.");
        }
        return unidade;
    }

    private Estoque buscarEstoque(Long id) {
        return estoqueRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ESTOQUE_NAO_ENCONTRADO,
                        "Estoque ID " + id + " nao encontrado."));
    }

    private void validarEstoqueNaoExistente(Long unidadeId, Long produtoId) {
        if (estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produtoId).isPresent()) {
            throw new RegraNegocioException(ESTOQUE_JA_EXISTE,
                    "Ja existe registro de estoque para este produto nesta unidade.");
        }
    }

    private EstoqueResponseDTO converterParaResponseDTO(Estoque estoque) {
        EstoqueResponseDTO response = new EstoqueResponseDTO();
        response.setId(estoque.getId());
        response.setProdutoId(estoque.getProduto().getId());
        response.setNomeProduto(estoque.getProduto().getNome());
        response.setUnidadeId(estoque.getUnidade().getId());
        response.setNomeUnidade(estoque.getUnidade().getNome());
        response.setQuantidade(estoque.getQuantidade());
        return response;
    }

    private MovimentacaoEstoqueResponseDTO converterMovimentacaoParaResponseDTO(MovimentacaoEstoque movimentacao) {
        MovimentacaoEstoqueResponseDTO response = new MovimentacaoEstoqueResponseDTO();
        response.setId(movimentacao.getId());
        response.setEstoqueId(movimentacao.getEstoque().getId());
        response.setProdutoId(movimentacao.getEstoque().getProduto().getId());
        response.setUnidadeId(movimentacao.getEstoque().getUnidade().getId());
        response.setTipo(movimentacao.getTipo());
        response.setQuantidade(movimentacao.getQuantidade());
        response.setSaldoAnterior(movimentacao.getSaldoAnterior());
        response.setSaldoPosterior(movimentacao.getSaldoPosterior());
        response.setMotivo(movimentacao.getMotivo());
        response.setAtor(movimentacao.getAtor());
        response.setCriadoEm(movimentacao.getCriadoEm());
        return response;
    }
}

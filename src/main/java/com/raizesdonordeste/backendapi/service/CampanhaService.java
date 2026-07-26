package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.CampanhaRequestDTO;
import com.raizesdonordeste.backendapi.dto.CampanhaResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Campanha;
import com.raizesdonordeste.backendapi.model.Produto;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.repository.CampanhaRepository;
import com.raizesdonordeste.backendapi.repository.ProdutoRepository;
import com.raizesdonordeste.backendapi.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampanhaService {

    private static final String CAMPANHA_NAO_ENCONTRADA = "CAMPANHA_NAO_ENCONTRADA";
    private static final String PRODUTO_NAO_ENCONTRADO = "PRODUTO_NAO_ENCONTRADO";
    private static final String UNIDADE_NAO_ENCONTRADA = "UNIDADE_NAO_ENCONTRADA";
    private static final String UNIDADE_INATIVA = "UNIDADE_INATIVA";
    private static final String DATA_INVALIDA = "DATA_INVALIDA";
    private static final String NOME_INVALIDO = "NOME_INVALIDO";
    private static final String PRECO_INVALIDO = "PRECO_INVALIDO";
    private static final String PRODUTO_INATIVO = "PRODUTO_INATIVO";
    private static final String CAMPANHA_SOBREPOSTA = "CAMPANHA_SOBREPOSTA";

    private final CampanhaRepository campanhaRepository;
    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;
    private final AcessoUnidadeService acessoUnidadeService;

    @Transactional
    public CampanhaResponseDTO criar(CampanhaRequestDTO dto) {
        validarDadosEntrada(dto);
        acessoUnidadeService.verificarAcessoGerencialUnidade(dto.getUnidadeId());
        validarNome(dto.getNome());
        validarPrecoPromocional(dto.getPrecoPromocional());
        validarPeriodo(dto.getDataInicio(), dto.getDataFim());

        Produto produto = buscarProdutoAtivo(dto.getProdutoId());
        Unidade unidade = buscarUnidade(dto.getUnidadeId());
        validarPrecoPromocionalMenorQueVigente(dto.getPrecoPromocional(), produto.getPrecoVigente());
        validarSobreposicaoCampanha(dto.getProdutoId(), dto.getUnidadeId(), 
                dto.getDataInicio(), dto.getDataFim(), null);

        Campanha campanha = new Campanha();
        campanha.setNome(dto.getNome().trim());
        campanha.setProduto(produto);
        campanha.setUnidade(unidade);
        campanha.setPrecoPromocional(dto.getPrecoPromocional());
        campanha.setDataInicio(dto.getDataInicio());
        campanha.setDataFim(dto.getDataFim());
        campanha.setAtiva(dto.getAtiva() != null ? dto.getAtiva() : Boolean.TRUE);

        Campanha campanhaSalva = campanhaRepository.save(campanha);

        log.info("Campanha {} criada | Nome: {} | Produto: {} | Preco: {} -> {}",
                campanhaSalva.getId(), campanhaSalva.getNome(), produto.getNome(),
                produto.getPrecoVigente(), campanhaSalva.getPrecoPromocional());

        return converterParaResponseDTO(campanhaSalva);
    }

    @Transactional(readOnly = true)
    public Page<CampanhaResponseDTO> listarTodas(Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable nao pode ser nulo");
        List<Long> unidadesPermitidas = acessoUnidadeService.getUnidadesGerenciaisDoUsuario();
        Page<Campanha> campanhas = unidadesPermitidas == null
                ? campanhaRepository.findAll(pageable)
                : campanhaRepository.findByUnidadeIdIn(unidadesPermitidas, pageable);
        log.info("Campanhas listadas | Total: {}", campanhas.getTotalElements());

        return campanhas.map(this::converterParaResponseDTO);
    }

    @Transactional(readOnly = true)
    public CampanhaResponseDTO buscarPorId(Long id) {
        Objects.requireNonNull(id, "ID da campanha nao pode ser nulo");
        Campanha campanha = buscarCampanha(id);
        acessoUnidadeService.verificarAcessoGerencialUnidade(campanha.getUnidade().getId());
        return converterParaResponseDTO(campanha);
    }

    @Transactional
    public CampanhaResponseDTO atualizar(Long id, CampanhaRequestDTO dto) {
        Objects.requireNonNull(id, "ID da campanha nao pode ser nulo");
        validarDadosEntrada(dto);
        validarNome(dto.getNome());
        validarPrecoPromocional(dto.getPrecoPromocional());
        validarPeriodo(dto.getDataInicio(), dto.getDataFim());

        Campanha campanha = buscarCampanha(id);
        acessoUnidadeService.verificarAcessoGerencialUnidade(campanha.getUnidade().getId());
        acessoUnidadeService.verificarAcessoGerencialUnidade(dto.getUnidadeId());
        Produto produto = buscarProdutoAtivo(dto.getProdutoId());
        Unidade unidade = buscarUnidade(dto.getUnidadeId());
        validarPrecoPromocionalMenorQueVigente(dto.getPrecoPromocional(), produto.getPrecoVigente());
        validarSobreposicaoCampanha(dto.getProdutoId(), dto.getUnidadeId(),
                dto.getDataInicio(), dto.getDataFim(), id);

        campanha.setNome(dto.getNome().trim());
        campanha.setProduto(produto);
        campanha.setUnidade(unidade);
        campanha.setPrecoPromocional(dto.getPrecoPromocional());
        campanha.setDataInicio(dto.getDataInicio());
        campanha.setDataFim(dto.getDataFim());

        if (dto.getAtiva() != null) {
            campanha.setAtiva(dto.getAtiva());
        }

        Campanha campanhaAtualizada = campanhaRepository.save(campanha);

        log.info("Campanha {} atualizada | Nome: {} | Preco: {}",
                id, campanhaAtualizada.getNome(), campanhaAtualizada.getPrecoPromocional());

        return converterParaResponseDTO(campanhaAtualizada);
    }

    @Transactional
    public void desativar(Long id) {
        Objects.requireNonNull(id, "ID da campanha nao pode ser nulo");
        Campanha campanha = buscarCampanha(id);
        acessoUnidadeService.verificarAcessoGerencialUnidade(campanha.getUnidade().getId());

        if (!Boolean.TRUE.equals(campanha.getAtiva())) {
            log.warn("Campanha {} ja esta inativa", id);
            return;
        }

        campanha.setAtiva(Boolean.FALSE);
        campanhaRepository.save(campanha);

        log.info("Campanha {} desativada | Nome: {}", id, campanha.getNome());
    }

    private void validarDadosEntrada(CampanhaRequestDTO dto) {
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        Objects.requireNonNull(dto.getProdutoId(), "ProdutoId e obrigatorio");
        Objects.requireNonNull(dto.getUnidadeId(), "UnidadeId e obrigatorio");
        Objects.requireNonNull(dto.getDataInicio(), "Data de inicio e obrigatoria");
        Objects.requireNonNull(dto.getDataFim(), "Data de fim e obrigatoria");
    }

    private void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new RegraNegocioException(NOME_INVALIDO,
                    "O nome da campanha e obrigatorio.");
        }
        if (nome.trim().length() < 3) {
            throw new RegraNegocioException(NOME_INVALIDO,
                    "O nome da campanha deve ter no minimo 3 caracteres.");
        }
    }

    private void validarPrecoPromocional(BigDecimal preco) {
        if (preco == null) {
            throw new RegraNegocioException(PRECO_INVALIDO,
                    "O preco promocional e obrigatorio.");
        }
        if (preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException(PRECO_INVALIDO,
                    "O preco promocional deve ser maior que zero.");
        }
    }

    private void validarPrecoPromocionalMenorQueVigente(BigDecimal promocional, BigDecimal vigente) {
        if (promocional.compareTo(vigente) >= 0) {
            throw new RegraNegocioException(PRECO_INVALIDO,
                    "O preco promocional deve ser menor que o preco vigente (" + vigente + ").");
        }
    }

    private void validarPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (!dataFim.isAfter(dataInicio)) {
            throw new RegraNegocioException(DATA_INVALIDA,
                    "A data de fim deve ser posterior a data de inicio.");
        }
    }

    private void validarSobreposicaoCampanha(Long produtoId, Long unidadeId,
                                              LocalDateTime dataInicio, LocalDateTime dataFim,
                                              Long campanhaIdAtual) {
        List<Campanha> campanhasExistentes = campanhaRepository
                .findByProdutoIdAndUnidadeIdAndAtivaTrue(produtoId, unidadeId);

        for (Campanha existente : campanhasExistentes) {
            if (campanhaIdAtual != null && existente.getId().equals(campanhaIdAtual)) {
                continue;
            }

            boolean sobreposicao = !dataFim.isBefore(existente.getDataInicio())
                    && !dataInicio.isAfter(existente.getDataFim());

            if (sobreposicao) {
                throw new RegraNegocioException(CAMPANHA_SOBREPOSTA,
                        "Ja existe uma campanha ativa para este produto e unidade no periodo informado: "
                                + existente.getNome());
            }
        }
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
        Unidade unidade = unidadeRepository.findByIdForUpdate(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(UNIDADE_NAO_ENCONTRADA,
                        "Unidade ID " + unidadeId + " nao encontrada."));
        if (!Boolean.TRUE.equals(unidade.getAtivo())) {
            throw new RegraNegocioException(UNIDADE_INATIVA,
                    "A unidade '" + unidade.getNome() + "' esta inativa e nao pode receber campanhas.");
        }
        return unidade;
    }

    private Campanha buscarCampanha(Long id) {
        return campanhaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(CAMPANHA_NAO_ENCONTRADA,
                        "Campanha ID " + id + " nao encontrada."));
    }

    private CampanhaResponseDTO converterParaResponseDTO(Campanha campanha) {
        CampanhaResponseDTO response = new CampanhaResponseDTO();
        response.setId(campanha.getId());
        response.setNome(campanha.getNome());
        response.setProdutoId(campanha.getProduto().getId());
        response.setNomeProduto(campanha.getProduto().getNome());
        response.setUnidadeId(campanha.getUnidade().getId());
        response.setNomeUnidade(campanha.getUnidade().getNome());
        response.setPrecoPromocional(campanha.getPrecoPromocional());
        response.setDataInicio(campanha.getDataInicio());
        response.setDataFim(campanha.getDataFim());
        response.setAtiva(campanha.getAtiva());
        return response;
    }
}

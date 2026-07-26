package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.CupomRequestDTO;
import com.raizesdonordeste.backendapi.dto.CupomResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Cupom;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.repository.CupomRepository;
import com.raizesdonordeste.backendapi.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CupomService {

	private static final String CUPOM_NAO_ENCONTRADO = "CUPOM_NAO_ENCONTRADO";
	private static final String CUPOM_JA_EXISTE = "CUPOM_JA_EXISTE";
	private static final String UNIDADE_NAO_ENCONTRADA = "UNIDADE_NAO_ENCONTRADA";
	private static final String UNIDADE_INATIVA = "UNIDADE_INATIVA";
	private static final String DATA_INVALIDA = "DATA_INVALIDA";
	private static final String VALOR_INVALIDO = "VALOR_INVALIDO";
	private static final String CODIGO_INVALIDO = "CODIGO_INVALIDO";
	private static final String CUPOM_EM_USO = "CUPOM_EM_USO";

    private final CupomRepository cupomRepository;
    private final UnidadeRepository unidadeRepository;
    private final AcessoUnidadeService acessoUnidadeService;

    @Transactional
    public CupomResponseDTO criar(CupomRequestDTO dto) {
        validarDadosEntrada(dto);
        if (dto.getUnidadeId() == null) {
            acessoUnidadeService.verificarAcessoAdministrativoGlobal();
        } else {
            acessoUnidadeService.verificarAcessoGerencialUnidade(dto.getUnidadeId());
        }
        validarCodigo(dto.getCodigo());
        validarValor(dto.getValor());
        validarValorMinimo(dto.getValorMinimoPedido());
        validarUsoMaximo(dto.getUsoMaximo());
        validarPeriodo(dto.getDataInicio(), dto.getDataFim());
        validarCodigoUnico(dto.getCodigo());

        Unidade unidade = buscarUnidadeSeInformada(dto.getUnidadeId());

        Cupom cupom = new Cupom();
        cupom.setCodigo(dto.getCodigo().trim().toUpperCase());
        cupom.setTipoDesconto(dto.getTipoDesconto());
        cupom.setValor(dto.getValor());
        cupom.setValorMinimoPedido(dto.getValorMinimoPedido() != null ? dto.getValorMinimoPedido() : BigDecimal.ZERO);
        cupom.setUnidade(unidade);
        cupom.setDataInicio(dto.getDataInicio());
        cupom.setDataFim(dto.getDataFim());
        cupom.setUsoMaximo(dto.getUsoMaximo());
        cupom.setUsoAtual(0);
        cupom.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : Boolean.TRUE);

        Cupom cupomSalvo = cupomRepository.save(cupom);

        log.info("Cupom {} criado | Codigo: {} | Tipo: {}",
                cupomSalvo.getId(), cupomSalvo.getCodigo(), cupomSalvo.getTipoDesconto());

        return converterParaResponseDTO(cupomSalvo);
    }

    @Transactional(readOnly = true)
    public Page<CupomResponseDTO> listarTodos(Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable nao pode ser nulo");
        List<Long> unidadesPermitidas = acessoUnidadeService.getUnidadesGerenciaisDoUsuario();
        Page<Cupom> cupons = unidadesPermitidas == null
                ? cupomRepository.findAll(pageable)
                : cupomRepository.findByUnidadeIdIn(unidadesPermitidas, pageable);
        log.info("Cupons listados | Total: {}", cupons.getTotalElements());

        return cupons.map(this::converterParaResponseDTO);
    }

    @Transactional(readOnly = true)
    public CupomResponseDTO buscarPorId(Long id) {
        Objects.requireNonNull(id, "ID do cupom nao pode ser nulo");
        Cupom cupom = buscarCupom(id);
        validarAcessoGerencial(cupom.getUnidade());
        return converterParaResponseDTO(cupom);
    }

    @Transactional
    public CupomResponseDTO atualizar(Long id, CupomRequestDTO dto) {
        Objects.requireNonNull(id, "ID do cupom nao pode ser nulo");
        validarDadosEntrada(dto);
        validarCodigo(dto.getCodigo());
        validarValor(dto.getValor());
        validarValorMinimo(dto.getValorMinimoPedido());
        validarUsoMaximo(dto.getUsoMaximo());
        validarPeriodo(dto.getDataInicio(), dto.getDataFim());

        Cupom cupom = buscarCupom(id);
        validarAcessoGerencial(cupom.getUnidade());

        String novoCodigo = dto.getCodigo().trim().toUpperCase();
        if (!cupom.getCodigo().equals(novoCodigo)) {
            if (cupom.getUsoAtual() > 0) {
                throw new RegraNegocioException(CUPOM_EM_USO,
                        "Nao e possivel alterar o codigo de um cupom que ja foi utilizado.");
            }
            validarCodigoUnico(novoCodigo);
        }

        Unidade unidade = buscarUnidadeSeInformada(dto.getUnidadeId());
        validarAcessoGerencial(unidade);

        cupom.setCodigo(novoCodigo);
        cupom.setTipoDesconto(dto.getTipoDesconto());
        cupom.setValor(dto.getValor());
        cupom.setValorMinimoPedido(dto.getValorMinimoPedido() != null ? dto.getValorMinimoPedido() : BigDecimal.ZERO);
        cupom.setUnidade(unidade);
        cupom.setDataInicio(dto.getDataInicio());
        cupom.setDataFim(dto.getDataFim());

        if (dto.getUsoMaximo() != null) {
            if (dto.getUsoMaximo() < cupom.getUsoAtual()) {
                throw new RegraNegocioException(VALOR_INVALIDO,
                        "O uso maximo nao pode ser menor que o uso atual (" + cupom.getUsoAtual() + ").");
            }
            cupom.setUsoMaximo(dto.getUsoMaximo());
        }

        if (dto.getAtivo() != null) {
            cupom.setAtivo(dto.getAtivo());
        }

        Cupom cupomAtualizado = cupomRepository.save(cupom);

        log.info("Cupom {} atualizado | Codigo: {}", id, cupomAtualizado.getCodigo());

        return converterParaResponseDTO(cupomAtualizado);
    }

    @Transactional
    public void desativar(Long id) {
        Objects.requireNonNull(id, "ID do cupom nao pode ser nulo");
        Cupom cupom = buscarCupom(id);
        validarAcessoGerencial(cupom.getUnidade());

        if (!Boolean.TRUE.equals(cupom.getAtivo())) {
            log.warn("Cupom {} ja esta inativo", id);
            return;
        }

        cupom.setAtivo(Boolean.FALSE);
        cupomRepository.save(cupom);

        log.info("Cupom {} desativado | Codigo: {}", id, cupom.getCodigo());
    }

    private void validarDadosEntrada(CupomRequestDTO dto) {
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        Objects.requireNonNull(dto.getTipoDesconto(), "Tipo de desconto e obrigatorio");
        Objects.requireNonNull(dto.getDataInicio(), "Data de inicio e obrigatoria");
        Objects.requireNonNull(dto.getDataFim(), "Data de fim e obrigatoria");
    }

    private void validarCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new RegraNegocioException(CODIGO_INVALIDO,
                    "O codigo do cupom e obrigatorio.");
        }
        if (codigo.trim().length() < 3) {
            throw new RegraNegocioException(CODIGO_INVALIDO,
                    "O codigo do cupom deve ter no minimo 3 caracteres.");
        }
        if (codigo.trim().length() > 50) {
            throw new RegraNegocioException(CODIGO_INVALIDO,
                    "O codigo do cupom deve ter no maximo 50 caracteres.");
        }
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null) {
            throw new RegraNegocioException(VALOR_INVALIDO,
                    "O valor do cupom e obrigatorio.");
        }
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException(VALOR_INVALIDO,
                    "O valor do cupom deve ser maior que zero.");
        }
    }

    private void validarValorMinimo(BigDecimal valorMinimo) {
        if (valorMinimo != null && valorMinimo.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraNegocioException(VALOR_INVALIDO,
                    "O valor minimo do pedido nao pode ser negativo.");
        }
    }

    private void validarUsoMaximo(Integer usoMaximo) {
        if (usoMaximo != null && usoMaximo <= 0) {
            throw new RegraNegocioException(VALOR_INVALIDO,
                    "O uso maximo deve ser maior que zero.");
        }
    }

    private void validarPeriodo(java.time.LocalDateTime dataInicio, java.time.LocalDateTime dataFim) {
        if (!dataFim.isAfter(dataInicio)) {
            throw new RegraNegocioException(DATA_INVALIDA,
                    "A data de fim deve ser posterior a data de inicio.");
        }
    }

    private void validarCodigoUnico(String codigo) {
        if (cupomRepository.findByCodigo(codigo.trim().toUpperCase()).isPresent()) {
            throw new RegraNegocioException(CUPOM_JA_EXISTE,
                    "Ja existe um cupom com o codigo: " + codigo);
        }
    }

    private Unidade buscarUnidadeSeInformada(Long unidadeId) {
        if (unidadeId == null) {
            return null;
        }
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(UNIDADE_NAO_ENCONTRADA,
                        "Unidade ID " + unidadeId + " nao encontrada."));
        if (!Boolean.TRUE.equals(unidade.getAtivo())) {
            throw new RegraNegocioException(UNIDADE_INATIVA,
                    "A unidade '" + unidade.getNome() + "' esta inativa e nao pode receber cupons.");
        }
        return unidade;
    }

    private void validarAcessoGerencial(Unidade unidade) {
        if (unidade == null) {
            acessoUnidadeService.verificarAcessoAdministrativoGlobal();
            return;
        }
        acessoUnidadeService.verificarAcessoGerencialUnidade(unidade.getId());
    }

    private Cupom buscarCupom(Long id) {
        return cupomRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(CUPOM_NAO_ENCONTRADO,
                        "Cupom ID " + id + " nao encontrado."));
    }

    private CupomResponseDTO converterParaResponseDTO(Cupom cupom) {
        CupomResponseDTO response = new CupomResponseDTO();
        response.setId(cupom.getId());
        response.setCodigo(cupom.getCodigo());
        response.setTipoDesconto(cupom.getTipoDesconto());
        response.setValor(cupom.getValor());
        response.setValorMinimoPedido(cupom.getValorMinimoPedido());

        if (cupom.getUnidade() != null) {
            response.setUnidadeId(cupom.getUnidade().getId());
            response.setNomeUnidade(cupom.getUnidade().getNome());
        }

        response.setDataInicio(cupom.getDataInicio());
        response.setDataFim(cupom.getDataFim());
        response.setUsoMaximo(cupom.getUsoMaximo());
        response.setUsoAtual(cupom.getUsoAtual());
        response.setAtivo(cupom.getAtivo());
        return response;
    }
}

package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.UnidadeDTO;
import com.raizesdonordeste.backendapi.dto.UnidadeResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnidadeService {

    private static final String UNIDADE_NAO_ENCONTRADA = "UNIDADE_NAO_ENCONTRADA";
    private static final String UNIDADE_JA_EXISTE = "UNIDADE_JA_EXISTE";
    private static final String UNIDADE_INATIVA = "UNIDADE_INATIVA";

    private final UnidadeRepository unidadeRepository;

    @Transactional
    public UnidadeResponseDTO criar(UnidadeDTO dto) {
        validarDadosEntrada(dto);
        validarNomeUnico(dto.getNome(), null);

        Unidade unidade = new Unidade();
        unidade.setNome(dto.getNome().trim());
        unidade.setEndereco(dto.getEndereco().trim());
        unidade.setCidade(dto.getCidade().trim());
        unidade.setEstado(dto.getEstado().trim().toUpperCase());
        unidade.setCep(dto.getCep().trim());
        unidade.setTelefone(dto.getTelefone().trim());
        unidade.setAtivo(Boolean.TRUE);

        Unidade unidadeSalva = unidadeRepository.save(unidade);

        log.info("Unidade {} criada | Nome: {} | Cidade: {}",
                unidadeSalva.getId(), unidadeSalva.getNome(), unidadeSalva.getCidade());

        return converterParaResponseDTO(unidadeSalva);
    }

    @Transactional(readOnly = true)
    public Page<UnidadeResponseDTO> listarTodas(Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable nao pode ser nulo");
        Page<Unidade> unidades = unidadeRepository.findByAtivoTrue(pageable);
        log.info("Unidades listadas | Total: {}", unidades.getTotalElements());
        return unidades.map(this::converterParaResponseDTO);
    }

    @Transactional(readOnly = true)
    public UnidadeResponseDTO buscarPorId(Long id) {
        Objects.requireNonNull(id, "ID da unidade nao pode ser nulo");
        Unidade unidade = buscarUnidade(id);
        validarUnidadeAtiva(unidade);
        return converterParaResponseDTO(unidade);
    }

    @Transactional
    public UnidadeResponseDTO atualizar(Long id, UnidadeDTO dto) {
        Objects.requireNonNull(id, "ID da unidade nao pode ser nulo");
        validarDadosEntrada(dto);

        Unidade unidade = buscarUnidade(id);
        validarUnidadeAtiva(unidade);
        validarNomeUnico(dto.getNome(), id);

        unidade.setNome(dto.getNome().trim());
        unidade.setEndereco(dto.getEndereco().trim());
        unidade.setCidade(dto.getCidade().trim());
        unidade.setEstado(dto.getEstado().trim().toUpperCase());
        unidade.setCep(dto.getCep().trim());
        unidade.setTelefone(dto.getTelefone().trim());

        Unidade unidadeAtualizada = unidadeRepository.save(unidade);

        log.info("Unidade {} atualizada | Nome: {}", id, unidadeAtualizada.getNome());

        return converterParaResponseDTO(unidadeAtualizada);
    }

    @Transactional
    public void desativar(Long id) {
        Objects.requireNonNull(id, "ID da unidade nao pode ser nulo");
        Unidade unidade = buscarUnidade(id);

        if (!Boolean.TRUE.equals(unidade.getAtivo())) {
            log.warn("Unidade {} ja esta inativa", id);
            return;
        }

        unidade.setAtivo(Boolean.FALSE);
        unidadeRepository.save(unidade);

        log.info("Unidade {} desativada (soft-delete) | Nome: {}", id, unidade.getNome());
    }

    private Unidade buscarUnidade(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(UNIDADE_NAO_ENCONTRADA,
                        "Unidade ID " + id + " nao encontrada."));
    }

    private void validarUnidadeAtiva(Unidade unidade) {
        if (!Boolean.TRUE.equals(unidade.getAtivo())) {
            throw new RegraNegocioException(UNIDADE_INATIVA,
                    "A unidade '" + unidade.getNome() + "' esta inativa e nao pode ser utilizada.");
        }
    }

    private void validarDadosEntrada(UnidadeDTO dto) {
        Objects.requireNonNull(dto, "DTO da unidade nao pode ser nulo");
    }

    private void validarNomeUnico(String nome, Long idExcluido) {
        String nomeTrim = nome.trim();
        boolean existe = unidadeRepository.findByNomeAndAtivoTrue(nomeTrim)
                .filter(u -> idExcluido == null || !u.getId().equals(idExcluido))
                .isPresent();

        if (existe) {
            throw new RegraNegocioException(UNIDADE_JA_EXISTE,
                    "Ja existe uma unidade ativa com o nome: " + nomeTrim);
        }
    }

    private UnidadeResponseDTO converterParaResponseDTO(Unidade unidade) {
        UnidadeResponseDTO response = new UnidadeResponseDTO();
        response.setId(unidade.getId());
        response.setNome(unidade.getNome());
        response.setEndereco(unidade.getEndereco());
        response.setCidade(unidade.getCidade());
        response.setEstado(unidade.getEstado());
        response.setCep(unidade.getCep());
        response.setTelefone(unidade.getTelefone());
        return response;
    }
}

package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Perfil;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import com.raizesdonordeste.backendapi.repository.UsuarioUnidadeRepository;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcessoUnidadeService {

    private static final String USUARIO_NAO_ENCONTRADO = "USUARIO_NAO_ENCONTRADO";
    private static final String AUTHENTICATION_NULA = "AUTHENTICATION_NULA";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioUnidadeRepository usuarioUnidadeRepository;

    public List<Long> getUnidadesDoUsuario() {
        Authentication authentication = obterAuthentication();
        String email = authentication.getName();

        Usuario usuario = buscarUsuario(email);

        if (temAcessoTotal(usuario.getPerfil())) {
            log.debug("UsuarioId {} tem acesso total (perfil: {})", usuario.getId(), usuario.getPerfil());
            return null;
        }

        List<Long> unidades = buscarUnidadesDoUsuario(email);
        log.debug("UsuarioId {} tem acesso a {} unidades", usuario.getId(), unidades.size());
        return unidades;
    }

    public void verificarAcessoUnidade(Long unidadeId) {
        Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");

        List<Long> unidadesPermitidas = getUnidadesDoUsuario();

        if (unidadesPermitidas == null) {
            return;
        }

        if (!unidadesPermitidas.contains(unidadeId)) {
            throw new AccessDeniedException("Voce nao tem acesso a esta unidade.");
        }

        log.debug("Acesso a unidade {} permitido", unidadeId);
    }

    public List<Long> getUnidadesGerenciaisDoUsuario() {
        Authentication authentication = obterAuthentication();
        Usuario usuario = buscarUsuario(authentication.getName());

        if (usuario.getPerfil() == Perfil.ADMIN) {
            return null;
        }
        if (usuario.getPerfil() != Perfil.GERENTE) {
            throw new AccessDeniedException("Apenas gerentes e administradores podem gerenciar este recurso.");
        }
        return buscarUnidadesDoUsuario(usuario.getEmail());
    }

    public void verificarAcessoGerencialUnidade(Long unidadeId) {
        Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");
        List<Long> unidadesPermitidas = getUnidadesGerenciaisDoUsuario();
        if (unidadesPermitidas != null && !unidadesPermitidas.contains(unidadeId)) {
            throw new AccessDeniedException("Voce nao tem permissao para gerenciar recursos desta unidade.");
        }
    }

    public void verificarAcessoAdministrativoGlobal() {
        Authentication authentication = obterAuthentication();
        Usuario usuario = buscarUsuario(authentication.getName());
        if (usuario.getPerfil() != Perfil.ADMIN) {
            throw new AccessDeniedException("Somente administradores podem gerenciar recursos globais.");
        }
    }

    private Authentication obterAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RegraNegocioException(AUTHENTICATION_NULA,
                    "Autenticacao nao encontrada no contexto de seguranca.");
        }

        return authentication;
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException(USUARIO_NAO_ENCONTRADO,
                        "Usuario nao encontrado."));
    }

    private boolean temAcessoTotal(Perfil perfil) {
        return perfil == Perfil.CLIENTE || perfil == Perfil.ADMIN;
    }

    private List<Long> buscarUnidadesDoUsuario(String email) {
        List<Long> unidades = usuarioUnidadeRepository.findByUsuarioEmail(email).stream()
                .map(uu -> uu.getUnidade().getId())
                .collect(Collectors.toList());

        if (unidades.isEmpty()) {
            log.warn("Ator {} nao possui unidades vinculadas", LogPseudonymizer.id(email));
            return Collections.emptyList();
        }

        return unidades;
    }
}

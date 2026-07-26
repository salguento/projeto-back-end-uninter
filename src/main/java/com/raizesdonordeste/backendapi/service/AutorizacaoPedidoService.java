package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.model.Pedido;
import com.raizesdonordeste.backendapi.model.Perfil;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import com.raizesdonordeste.backendapi.repository.UsuarioUnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AutorizacaoPedidoService {

    private static final String USUARIO_NAO_ENCONTRADO = "USUARIO_NAO_ENCONTRADO";
    private static final String ACESSO_NEGADO = "Voce nao tem permissao para acessar este pedido.";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioUnidadeRepository usuarioUnidadeRepository;

    public EscopoAcesso obterEscopo(String emailUsuario) {
        Objects.requireNonNull(emailUsuario, "Email do usuario nao pode ser nulo");

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        USUARIO_NAO_ENCONTRADO, "Usuario autenticado nao encontrado."));

        List<Long> unidadeIds = usuarioUnidadeRepository.findByUsuarioId(usuario.getId()).stream()
                .map(vinculo -> vinculo.getUnidade().getId())
                .distinct()
                .toList();

        return new EscopoAcesso(usuario.getId(), usuario.getPerfil(), unidadeIds);
    }

    public void verificarConsulta(Pedido pedido, String emailUsuario) {
        EscopoAcesso escopo = obterEscopo(emailUsuario);

        if (escopo.isAdmin()) {
            return;
        }
        if (escopo.isCliente() && pertenceAoCliente(pedido, escopo.usuarioId())) {
            return;
        }
        if (escopo.isFuncionario() && pertenceAUnidade(pedido, escopo.unidadeIds())) {
            return;
        }

        throw new AccessDeniedException(ACESSO_NEGADO);
    }

    public void verificarPagamento(Pedido pedido, String emailUsuario) {
        EscopoAcesso escopo = obterEscopo(emailUsuario);

        if (escopo.perfil() == Perfil.CLIENTE && pertenceAoCliente(pedido, escopo.usuarioId())) {
            return;
        }
        if (escopo.perfil() == Perfil.ATENDENTE && pertenceAUnidade(pedido, escopo.unidadeIds())) {
            return;
        }

        throw new AccessDeniedException(ACESSO_NEGADO);
    }

    public void verificarCancelamento(Pedido pedido, String emailUsuario) {
        EscopoAcesso escopo = obterEscopo(emailUsuario);

        if (escopo.isAdmin()) {
            return;
        }
        if (escopo.isCliente() && pertenceAoCliente(pedido, escopo.usuarioId())) {
            return;
        }
        if (escopo.perfil() == Perfil.ATENDENTE && pertenceAUnidade(pedido, escopo.unidadeIds())) {
            return;
        }

        throw new AccessDeniedException(ACESSO_NEGADO);
    }

    public void verificarOperacao(Pedido pedido, String emailUsuario) {
        EscopoAcesso escopo = obterEscopo(emailUsuario);

        if (escopo.isAdmin()) {
            return;
        }
        if (escopo.isFuncionario() && pertenceAUnidade(pedido, escopo.unidadeIds())) {
            return;
        }

        throw new AccessDeniedException(ACESSO_NEGADO);
    }

    private boolean pertenceAoCliente(Pedido pedido, Long usuarioId) {
        return pedido != null
                && pedido.getUsuario() != null
                && Objects.equals(pedido.getUsuario().getId(), usuarioId);
    }

    private boolean pertenceAUnidade(Pedido pedido, List<Long> unidadeIds) {
        return pedido != null
                && pedido.getUnidade() != null
                && unidadeIds.contains(pedido.getUnidade().getId());
    }

    public record EscopoAcesso(Long usuarioId, Perfil perfil, List<Long> unidadeIds) {
        public EscopoAcesso {
            unidadeIds = unidadeIds == null ? List.of() : List.copyOf(unidadeIds);
        }

        public boolean isAdmin() {
            return perfil == Perfil.ADMIN;
        }

        public boolean isCliente() {
            return perfil == Perfil.CLIENTE;
        }

        public boolean isFuncionario() {
            return perfil == Perfil.ATENDENTE || perfil == Perfil.COZINHA || perfil == Perfil.GERENTE;
        }
    }
}

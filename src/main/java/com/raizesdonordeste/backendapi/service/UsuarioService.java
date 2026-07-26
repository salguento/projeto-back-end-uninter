package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.CadastroUsuarioDTO;
import com.raizesdonordeste.backendapi.dto.UsuarioDTO;
import com.raizesdonordeste.backendapi.dto.UsuarioResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Perfil;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String EMAIL_DUPLICADO = "EMAIL_DUPLICADO";
    private static final String PERFIL_INVALIDO = "PERFIL_INVALIDO";
    private static final String USUARIO_NAO_ENCONTRADO = "USUARIO_NAO_ENCONTRADO";
    private static final String CONTA_ANONIMIZADA = "CONTA_ANONIMIZADA";
    private static final String EMAIL_INVALIDO = "EMAIL_INVALIDO";
    private static final String SENHA_INVALIDA = "SENHA_INVALIDA";
    private static final String NOME_INVALIDO = "NOME_INVALIDO";

    private static final String NOME_ANONIMIZADO = "USUARIO ANONIMIZADO";
    private static final String PREFIXO_EMAIL_ANONIMIZADO = "anonimizado-";
    private static final String SUFIXO_EMAIL_ANONIMIZADO = "@deleted.local";
    private static final String PREFIXO_SENHA_DESATIVADA = "CONTA_DESATIVADA_";

    private static final int TAMANHO_MINIMO_NOME = 3;
    private static final int TAMANHO_MINIMO_SENHA = 6;
    private static final int TAMANHO_UUID_ANONIMIZACAO = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentoLegalService documentoLegalService;

    @Transactional
    public UsuarioResponseDTO criar(CadastroUsuarioDTO dto) {
        validarDadosCriacao(dto);
        documentoLegalService.validarDadosAceiteCadastro(dto);
        validarEmailUnico(dto.getEmail());

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome().trim());
        usuario.setEmail(dto.getEmail().trim().toLowerCase());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setPontos(0);
        usuario.setTelefone(dto.getTelefone());
        usuario.setCpf(dto.getCpf());
        usuario.setAnonimizado(Boolean.FALSE);

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        documentoLegalService.registrarAceiteCadastro(usuarioSalvo, dto);

        log.info("Usuario {} criado | Perfil: CLIENTE", usuarioSalvo.getId());

        return converterParaResponseDTO(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> listarTodos(Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable nao pode ser nulo");
        return usuarioRepository.findAll(pageable).map(this::converterParaResponseDTO);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        Objects.requireNonNull(id, "ID do usuario nao pode ser nulo");
        Usuario usuario = buscarUsuario(id);
        return converterParaResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioDTO dto, String emailAutenticado) {
        Objects.requireNonNull(id, "ID do usuario nao pode ser nulo");
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        Objects.requireNonNull(emailAutenticado, "Email autenticado nao pode ser nulo");
        validarNome(dto.getNome());
        validarEmail(dto.getEmail());

        Usuario usuario = buscarUsuario(id);
        validarContaNaoAnonimizada(usuario);
        validarPermissaoEdicao(usuario, emailAutenticado);

        usuario.setNome(dto.getNome().trim());
        usuario.setTelefone(dto.getTelefone());
        usuario.setCpf(dto.getCpf());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            validarSenha(dto.getSenha());
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        if (dto.getPerfil() != null && dto.getPerfil() != usuario.getPerfil()) {
            validarPermissaoAlteracaoPerfil(emailAutenticado);
            usuario.setPerfil(dto.getPerfil());
        }

        String novoEmail = dto.getEmail().trim().toLowerCase();
        if (!usuario.getEmail().equals(novoEmail)) {
            validarEmailUnico(novoEmail);
            usuario.setEmail(novoEmail);
        }

        Usuario usuarioAtualizado = usuarioRepository.save(usuario);

        log.info("Usuario {} atualizado", id);

        return converterParaResponseDTO(usuarioAtualizado);
    }

    @Transactional
    public void deletar(Long id, String emailAutenticado) {
        Objects.requireNonNull(id, "ID do usuario nao pode ser nulo");
        Objects.requireNonNull(emailAutenticado, "Email autenticado nao pode ser nulo");

        Usuario usuario = buscarUsuario(id);
        validarPermissaoExclusao(usuario, emailAutenticado);

        anonimizarUsuario(usuario);

        log.info("Usuario {} anonimizado (LGPD)", id);
    }

    private void validarDadosCriacao(CadastroUsuarioDTO dto) {
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        validarNome(dto.getNome());
        validarEmail(dto.getEmail());
        validarSenha(dto.getSenha());

        if (dto.getPerfil() != null && dto.getPerfil() != Perfil.CLIENTE) {
            throw new RegraNegocioException(PERFIL_INVALIDO,
                    "No registro publico, apenas o perfil CLIENTE e permitido.");
        }
    }

    private void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new RegraNegocioException(NOME_INVALIDO,
                    "O nome do usuario e obrigatorio.");
        }
        if (nome.trim().length() < TAMANHO_MINIMO_NOME) {
            throw new RegraNegocioException(NOME_INVALIDO,
                    "O nome do usuario deve ter no minimo " + TAMANHO_MINIMO_NOME + " caracteres.");
        }
    }

    private void validarEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RegraNegocioException(EMAIL_INVALIDO,
                    "O email do usuario e obrigatorio.");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new RegraNegocioException(EMAIL_INVALIDO,
                    "O formato do email e invalido.");
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException(SENHA_INVALIDA,
                    "A senha deve ter no minimo " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RegraNegocioException(EMAIL_DUPLICADO,
                    "Ja existe um usuario cadastrado com este e-mail.");
        }
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(USUARIO_NAO_ENCONTRADO,
                        "Usuario ID " + id + " nao encontrado."));
    }

    private void validarContaNaoAnonimizada(Usuario usuario) {
        if (Boolean.TRUE.equals(usuario.getAnonimizado())) {
            throw new RegraNegocioException(CONTA_ANONIMIZADA,
                    "Esta conta foi anonimizada e nao pode ser modificada.");
        }
    }

    private void validarPermissaoEdicao(Usuario usuario, String emailAutenticado) {
        if (!usuario.getEmail().equals(emailAutenticado) && !isAdmin(emailAutenticado)) {
            throw new AccessDeniedException("Voce so pode editar sua propria conta.");
        }
    }

    private void validarPermissaoAlteracaoPerfil(String emailAutenticado) {
        if (!isAdmin(emailAutenticado)) {
            throw new AccessDeniedException("Somente administradores podem alterar perfis de usuario.");
        }
    }

    private void validarPermissaoExclusao(Usuario usuario, String emailAutenticado) {
        if (!usuario.getEmail().equals(emailAutenticado) && !isAdmin(emailAutenticado)) {
            throw new AccessDeniedException("Voce so pode excluir sua propria conta.");
        }
    }

    private boolean isAdmin(String email) {
        return usuarioRepository.findByEmail(email)
                .map(u -> u.getPerfil() == Perfil.ADMIN)
                .orElse(Boolean.FALSE);
    }

    private void anonimizarUsuario(Usuario usuario) {
        String uuid = UUID.randomUUID().toString().substring(0, TAMANHO_UUID_ANONIMIZACAO);

        usuario.setNome(NOME_ANONIMIZADO);
        usuario.setEmail(PREFIXO_EMAIL_ANONIMIZADO + uuid + SUFIXO_EMAIL_ANONIMIZADO);
        usuario.setSenha(passwordEncoder.encode(PREFIXO_SENHA_DESATIVADA + uuid));
        usuario.setCpf(null);
        usuario.setTelefone(null);
        usuario.setPontos(0);
        usuario.setAnonimizado(Boolean.TRUE);

        usuarioRepository.save(usuario);
    }

    private UsuarioResponseDTO converterParaResponseDTO(Usuario usuario) {
        UsuarioResponseDTO response = new UsuarioResponseDTO();
        response.setId(usuario.getId());
        response.setNome(usuario.getNome());
        response.setEmail(usuario.getEmail());
        response.setPerfil(usuario.getPerfil());
        response.setPontos(usuario.getPontos());
        response.setTelefone(usuario.getTelefone());
        response.setCpf(usuario.getCpf());
        return response;
    }
}

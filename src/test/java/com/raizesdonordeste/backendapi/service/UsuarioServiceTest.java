package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.CadastroUsuarioDTO;
import com.raizesdonordeste.backendapi.dto.UsuarioDTO;
import com.raizesdonordeste.backendapi.dto.UsuarioResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Perfil;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Testes de Lógica de Negócio")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DocumentoLegalService documentoLegalService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario cliente;
    private Usuario admin;
    private CadastroUsuarioDTO cadastroDTO;

    @BeforeEach
    void setUp() {
        cliente = new Usuario();
        cliente.setId(1L);
        cliente.setEmail("cliente@raizes.com");
        cliente.setNome("Cliente Teste");
        cliente.setPerfil(Perfil.CLIENTE);
        cliente.setPontos(0);
        cliente.setTelefone("11999999999");
        cliente.setCpf("12345678901");
        cliente.setAnonimizado(false);

        admin = new Usuario();
        admin.setId(2L);
        admin.setEmail("admin@raizes.com");
        admin.setNome("Administrador");
        admin.setPerfil(Perfil.ADMIN);
        admin.setPontos(0);
        admin.setAnonimizado(false);

        cadastroDTO = novoCadastro("Cliente Teste", "cliente@raizes.com", Perfil.CLIENTE);
        cadastroDTO.setTelefone("11999999999");
        cadastroDTO.setCpf("12345678901");
    }

    // ============================================
    // TESTES DE CRIAÇÃO
    // ============================================

    @Test
    @DisplayName("Deve criar usuário CLIENTE com sucesso")
    void deveCriarUsuarioClienteComSucesso() {
        CadastroUsuarioDTO novoDTO = novoCadastro("Novo Cliente", "novo@raizes.com", Perfil.CLIENTE);

        when(usuarioRepository.findByEmail("novo@raizes.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn("hash_senha123");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UsuarioResponseDTO response = usuarioService.criar(novoDTO);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Novo Cliente", response.getNome());
        assertEquals(Perfil.CLIENTE, response.getPerfil());
        verify(usuarioRepository).save(any(Usuario.class));
        verify(documentoLegalService).validarDadosAceiteCadastro(novoDTO);
        verify(documentoLegalService).registrarAceiteCadastro(any(Usuario.class), same(novoDTO));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar ADMIN via endpoint público")
    void deveLancarExcecaoAoCriarAdmin() {
        CadastroUsuarioDTO adminDTO = novoCadastro("Tentativa Admin", "admin@raizes.com", Perfil.ADMIN);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> usuarioService.criar(adminDTO));

        assertEquals("PERFIL_INVALIDO", exception.getErrorCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe")
    void deveLancarExcecaoQuandoEmailJaExiste() {
        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> usuarioService.criar(cadastroDTO));

        assertEquals("EMAIL_DUPLICADO", exception.getErrorCode());
        verify(usuarioRepository, never()).save(any());
    }

    private CadastroUsuarioDTO novoCadastro(String nome, String email, Perfil perfil) {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO();
        dto.setNome(nome);
        dto.setEmail(email);
        dto.setSenha("senha123");
        dto.setPerfil(perfil);
        dto.setAceiteTermosUso(true);
        dto.setVersaoTermosUso("1.0");
        dto.setHashTermosUso("a".repeat(64));
        return dto;
    }

    // ============================================
    // TESTES DE LISTAGEM
    // ============================================

    @Test
    @DisplayName("Deve listar todos os usuários com paginação")
    void deveListarTodosUsuariosComPaginacao() {
        Page<Usuario> page = new PageImpl<>(List.of(cliente, admin));
        Pageable pageable = PageRequest.of(0, 10);

        when(usuarioRepository.findAll(pageable)).thenReturn(page);

        Page<UsuarioResponseDTO> resultado = usuarioService.listarTodos(pageable);

        assertNotNull(resultado);
        assertEquals(2, resultado.getTotalElements());
    }

    // ============================================
    // TESTES DE ATUALIZAÇÃO
    // ============================================

    @Test
    @DisplayName("Deve permitir usuário editar sua própria conta")
    void devePermitirUsuarioEditarPropriaConta() {
        UsuarioDTO dtoAtualizado = new UsuarioDTO();
        dtoAtualizado.setNome("Cliente Atualizado");
        dtoAtualizado.setEmail("cliente@raizes.com");
        dtoAtualizado.setSenha("novaSenha123");
        dtoAtualizado.setPerfil(Perfil.CLIENTE);
        dtoAtualizado.setTelefone("11912345678");
        dtoAtualizado.setCpf("12345678901");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash_novaSenha123");
        when(usuarioRepository.save(cliente)).thenReturn(cliente);

        UsuarioResponseDTO response = usuarioService.atualizar(1L, dtoAtualizado, "cliente@raizes.com");

        assertNotNull(response);
        assertEquals("Cliente Atualizado", response.getNome());
        assertEquals("11912345678", response.getTelefone());
        verify(usuarioRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve negar alteração do próprio perfil por usuário não administrador")
    void deveNegarAlteracaoDoProprioPerfilPorUsuarioNaoAdministrador() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("Cliente Teste");
        dto.setEmail("cliente@raizes.com");
        dto.setSenha("senha123");
        dto.setPerfil(Perfil.ADMIN);
        dto.setTelefone("11999999999");
        dto.setCpf("12345678901");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> usuarioService.atualizar(1L, dto, "cliente@raizes.com"));

        assertTrue(exception.getMessage().contains("administradores"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir ADMIN editar qualquer conta")
    void devePermitirAdminEditarQualquerConta() {
        UsuarioDTO dtoAtualizado = new UsuarioDTO();
        dtoAtualizado.setNome("Cliente Editado por Admin");
        dtoAtualizado.setEmail("cliente@raizes.com");
        dtoAtualizado.setSenha("novaSenha123");
        dtoAtualizado.setPerfil(Perfil.CLIENTE);
        dtoAtualizado.setTelefone("11912345678");
        dtoAtualizado.setCpf("12345678901");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByEmail("admin@raizes.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash_novaSenha123");
        when(usuarioRepository.save(cliente)).thenReturn(cliente);

        UsuarioResponseDTO response = usuarioService.atualizar(1L, dtoAtualizado, "admin@raizes.com");

        assertNotNull(response);
        assertEquals("Cliente Editado por Admin", response.getNome());
        verify(usuarioRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar email para um já existente")
    void deveLancarExcecaoAoAtualizarEmailParaUmJaExistente() {
        Usuario outroUsuario = new Usuario();
        outroUsuario.setId(2L);
        outroUsuario.setEmail("outro@raizes.com");

        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("Cliente Teste");
        dto.setEmail("outro@raizes.com");
        dto.setSenha("senha123");
        dto.setPerfil(Perfil.CLIENTE);
        dto.setTelefone("11999999999");
        dto.setCpf("12345678901");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByEmail("outro@raizes.com")).thenReturn(Optional.of(outroUsuario));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> usuarioService.atualizar(1L, dto, "cliente@raizes.com"));

        assertEquals("EMAIL_DUPLICADO", exception.getErrorCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar editar conta anonimizada")
    void deveLancarExcecaoAoEditarContaAnonimizada() {
        cliente.setAnonimizado(true);

        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("Tentativa");
        dto.setEmail("cliente@raizes.com");
        dto.setSenha("senha123");
        dto.setPerfil(Perfil.CLIENTE);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> usuarioService.atualizar(1L, dto, "cliente@raizes.com"));

        assertEquals("CONTA_ANONIMIZADA", exception.getErrorCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário tenta editar conta de outro")
    void deveLancarExcecaoQuandoUsuarioTentaEditarContaDeOutro() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("Tentativa");
        dto.setEmail("cliente@raizes.com");
        dto.setSenha("senha123");
        dto.setPerfil(Perfil.CLIENTE);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> usuarioService.atualizar(2L, dto, "cliente@raizes.com"));

        assertTrue(exception.getMessage().contains("propria conta"));
        verify(usuarioRepository, never()).save(any());
    }

    // ============================================
    // TESTES DE EXCLUSÃO (LGPD)
    // ============================================

    @Test
    @DisplayName("Deve anonimizar usuario ao deletar (LGPD)")
    void deveAnonimizarUsuarioAoDeletar() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(passwordEncoder.encode(anyString())).thenReturn("hash_conta_desativada");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        usuarioService.deletar(1L, "cliente@raizes.com");

        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository).save(argThat(u -> 
            u.getNome().equals("USUARIO ANONIMIZADO") &&
            u.getEmail().startsWith("anonimizado-") &&
            u.getEmail().endsWith("@deleted.local") &&
            u.getCpf() == null &&
            u.getTelefone() == null &&
            u.getPontos() == 0 &&
            Boolean.TRUE.equals(u.getAnonimizado())
        ));
        verify(usuarioRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário tenta deletar conta de outro")
    void deveLancarExcecaoQuandoUsuarioTentaDeletarContaDeOutro() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> usuarioService.deletar(2L, "cliente@raizes.com"));

        assertTrue(exception.getMessage().contains("propria conta"));
        verify(usuarioRepository, never()).save(any());
    }
}

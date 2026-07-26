package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.model.Perfil;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.model.UsuarioUnidade;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import com.raizesdonordeste.backendapi.repository.UsuarioUnidadeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AcessoUnidadeService - Testes de Controle de Acesso por Unidade")
class AcessoUnidadeServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioUnidadeRepository usuarioUnidadeRepository;

    @InjectMocks
    private AcessoUnidadeService acessoUnidadeService;

    private Usuario admin;
    private Usuario cliente;
    private Usuario atendente;
    private Usuario cozinha;
    private Usuario gerente;

    private Unidade unidade1;
    private Unidade unidade2;

    @BeforeEach
    void setUp() {
        // Configurar usuários com diferentes perfis
        admin = new Usuario();
        admin.setId(1L);
        admin.setEmail("admin@raizes.com");
        admin.setPerfil(Perfil.ADMIN);

        cliente = new Usuario();
        cliente.setId(2L);
        cliente.setEmail("cliente@raizes.com");
        cliente.setPerfil(Perfil.CLIENTE);

        atendente = new Usuario();
        atendente.setId(3L);
        atendente.setEmail("atendente@raizes.com");
        atendente.setPerfil(Perfil.ATENDENTE);

        cozinha = new Usuario();
        cozinha.setId(4L);
        cozinha.setEmail("cozinha@raizes.com");
        cozinha.setPerfil(Perfil.COZINHA);

        gerente = new Usuario();
        gerente.setId(5L);
        gerente.setEmail("gerente@raizes.com");
        gerente.setPerfil(Perfil.GERENTE);

        // Configurar unidades
        unidade1 = new Unidade();
        unidade1.setId(1L);
        unidade1.setNome("Unidade Centro");

        unidade2 = new Unidade();
        unidade2.setId(2L);
        unidade2.setNome("Unidade Zona Sul");
    }

    @AfterEach
    void tearDown() {
        // Limpar contexto de segurança após cada teste
        SecurityContextHolder.clearContext();
    }

    // Helper para configurar Authentication no SecurityContext
    private void configurarAuthentication(String email, Perfil perfil) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + perfil.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ============================================
    // TESTES DE getUnidadesDoUsuario()
    // ============================================

    @Test
    @DisplayName("Deve retornar null quando usuário é ADMIN (acesso total)")
    void deveRetornarNullQuandoAdmin() {
        // Arrange
        configurarAuthentication("admin@raizes.com", Perfil.ADMIN);
        when(usuarioRepository.findByEmail("admin@raizes.com")).thenReturn(Optional.of(admin));

        // Act
        List<Long> resultado = acessoUnidadeService.getUnidadesDoUsuario();

        // Assert
        assertNull(resultado);
        verify(usuarioRepository).findByEmail("admin@raizes.com");
        verify(usuarioUnidadeRepository, never()).findByUsuarioEmail(anyString());
    }

    @Test
    @DisplayName("Deve retornar lista de unidades quando usuário é ATENDENTE")
    void deveRetornarListaDeUnidadesQuandoAtendente() {
        // Arrange
        configurarAuthentication("atendente@raizes.com", Perfil.ATENDENTE);
        when(usuarioRepository.findByEmail("atendente@raizes.com")).thenReturn(Optional.of(atendente));

        UsuarioUnidade uu1 = new UsuarioUnidade();
        uu1.setUnidade(unidade1);

        UsuarioUnidade uu2 = new UsuarioUnidade();
        uu2.setUnidade(unidade2);

        when(usuarioUnidadeRepository.findByUsuarioEmail("atendente@raizes.com"))
                .thenReturn(List.of(uu1, uu2));

        // Act
        List<Long> resultado = acessoUnidadeService.getUnidadesDoUsuario();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(1L));
        assertTrue(resultado.contains(2L));
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não é encontrado")
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        // Arrange
        configurarAuthentication("inexistente@raizes.com", Perfil.CLIENTE);
        when(usuarioRepository.findByEmail("inexistente@raizes.com")).thenReturn(Optional.empty());

        // Act & Assert
        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> acessoUnidadeService.getUnidadesDoUsuario());

        assertEquals("USUARIO_NAO_ENCONTRADO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando funcionário não tem unidades vinculadas")
    void deveRetornarListaVaziaQuandoSemUnidades() {
        // Arrange
        configurarAuthentication("atendente@raizes.com", Perfil.ATENDENTE);
        when(usuarioRepository.findByEmail("atendente@raizes.com")).thenReturn(Optional.of(atendente));
        when(usuarioUnidadeRepository.findByUsuarioEmail("atendente@raizes.com"))
                .thenReturn(Collections.emptyList());

        // Act
        List<Long> resultado = acessoUnidadeService.getUnidadesDoUsuario();

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ============================================
    // TESTES DE verificarAcessoUnidade()
    // ============================================

    @Test
    @DisplayName("Deve permitir acesso quando usuário é ADMIN (unidade qualquer)")
    void devePermitirAcessoQuandoAdmin() {
        // Arrange
        configurarAuthentication("admin@raizes.com", Perfil.ADMIN);
        when(usuarioRepository.findByEmail("admin@raizes.com")).thenReturn(Optional.of(admin));

        // Act & Assert - Não deve lançar exceção
        assertDoesNotThrow(() -> acessoUnidadeService.verificarAcessoUnidade(999L));
    }

    @Test
    @DisplayName("Deve permitir acesso quando usuário é CLIENTE (unidade qualquer)")
    void devePermitirAcessoQuandoCliente() {
        // Arrange
        configurarAuthentication("cliente@raizes.com", Perfil.CLIENTE);
        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));

        // Act & Assert - Não deve lançar exceção
        assertDoesNotThrow(() -> acessoUnidadeService.verificarAcessoUnidade(999L));
    }

    @Test
    @DisplayName("Deve permitir acesso quando unidade está na lista permitida")
    void devePermitirAcessoQuandoUnidadePermitida() {
        // Arrange
        configurarAuthentication("atendente@raizes.com", Perfil.ATENDENTE);
        when(usuarioRepository.findByEmail("atendente@raizes.com")).thenReturn(Optional.of(atendente));

        UsuarioUnidade uu1 = new UsuarioUnidade();
        uu1.setUnidade(unidade1);

        when(usuarioUnidadeRepository.findByUsuarioEmail("atendente@raizes.com"))
                .thenReturn(List.of(uu1));

        // Act & Assert - Não deve lançar exceção
        assertDoesNotThrow(() -> acessoUnidadeService.verificarAcessoUnidade(1L));
    }

    @Test
    @DisplayName("Deve lançar exceção quando unidade NÃO está na lista permitida")
    void deveLancarExcecaoQuandoUnidadeNaoPermitida() {
        // Arrange
        configurarAuthentication("atendente@raizes.com", Perfil.ATENDENTE);
        when(usuarioRepository.findByEmail("atendente@raizes.com")).thenReturn(Optional.of(atendente));

        UsuarioUnidade uu1 = new UsuarioUnidade();
        uu1.setUnidade(unidade1);

        when(usuarioUnidadeRepository.findByUsuarioEmail("atendente@raizes.com"))
                .thenReturn(List.of(uu1));

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> acessoUnidadeService.verificarAcessoUnidade(2L));

        assertTrue(exception.getMessage().contains("unidade"));
    }

    @Test
    @DisplayName("Gerente deve administrar somente as unidades vinculadas")
    void gerenteDeveAdministrarSomenteUnidadesVinculadas() {
        configurarAuthentication("gerente@raizes.com", Perfil.GERENTE);
        when(usuarioRepository.findByEmail("gerente@raizes.com")).thenReturn(Optional.of(gerente));

        UsuarioUnidade vinculo = new UsuarioUnidade();
        vinculo.setUnidade(unidade1);
        when(usuarioUnidadeRepository.findByUsuarioEmail("gerente@raizes.com"))
                .thenReturn(List.of(vinculo));

        assertDoesNotThrow(() -> acessoUnidadeService.verificarAcessoGerencialUnidade(1L));
        assertThrows(AccessDeniedException.class,
                () -> acessoUnidadeService.verificarAcessoGerencialUnidade(2L));
    }

    @Test
    @DisplayName("Somente administrador deve gerenciar recursos globais")
    void somenteAdministradorDeveGerenciarRecursosGlobais() {
        configurarAuthentication("gerente@raizes.com", Perfil.GERENTE);
        when(usuarioRepository.findByEmail("gerente@raizes.com")).thenReturn(Optional.of(gerente));
        assertThrows(AccessDeniedException.class,
                () -> acessoUnidadeService.verificarAcessoAdministrativoGlobal());

        configurarAuthentication("admin@raizes.com", Perfil.ADMIN);
        when(usuarioRepository.findByEmail("admin@raizes.com")).thenReturn(Optional.of(admin));
        assertDoesNotThrow(() -> acessoUnidadeService.verificarAcessoAdministrativoGlobal());
    }
}

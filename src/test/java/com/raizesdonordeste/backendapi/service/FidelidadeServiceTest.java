package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.ConsentimentoResponseDTO;
import com.raizesdonordeste.backendapi.dto.SaldoPontosResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.ConsentimentoLgpd;
import com.raizesdonordeste.backendapi.model.FinalidadeConsentimento;
import com.raizesdonordeste.backendapi.model.Perfil;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.repository.ConsentimentoLgpdRepository;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FidelidadeService - Testes de Lógica de Negócio")
class FidelidadeServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConsentimentoLgpdRepository consentimentoLgpdRepository;

    @InjectMocks
    private FidelidadeService fidelidadeService;

    private Usuario cliente;

    @BeforeEach
    void setUp() {
        cliente = new Usuario();
        cliente.setId(1L);
        cliente.setEmail("cliente@raizes.com");
        cliente.setNome("Cliente Teste");
        cliente.setPerfil(Perfil.CLIENTE);
        cliente.setPontos(100);
        ReflectionTestUtils.setField(fidelidadeService, "versaoTermoFidelidade", "1.0");
    }

    // ============================================
    // TESTES DE SUCESSO
    // ============================================

    @Test
    @DisplayName("Deve consultar saldo de pontos com sucesso quando usuário existe")
    void deveConsultarSaldoComSucesso() {
        // Arrange
        when(usuarioRepository.findByEmail("cliente@raizes.com"))
                .thenReturn(Optional.of(cliente));

        // Act
        SaldoPontosResponseDTO response = fidelidadeService.consultarSaldo("cliente@raizes.com");

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getUsuarioId());
        assertEquals("cliente@raizes.com", response.getEmail());
        assertEquals(100, response.getSaldoPontos());
        assertEquals(10.0, response.getValorEstimadoEmDesconto());

        verify(usuarioRepository).findByEmail("cliente@raizes.com");
    }

    @Test
    @DisplayName("Deve informar saldo devedor sem disponibiliza-lo como desconto")
    void deveConsultarSaldoDevedorComValorDisponivelZero() {
        cliente.setPontos(-15);
        when(usuarioRepository.findByEmail("cliente@raizes.com"))
                .thenReturn(Optional.of(cliente));

        SaldoPontosResponseDTO response = fidelidadeService.consultarSaldo("cliente@raizes.com");

        assertEquals(-15, response.getSaldoPontos());
        assertEquals(0.0, response.getValorEstimadoEmDesconto());
    }

    @Test
    @DisplayName("Deve calcular valor estimado corretamente para múltiplos pontos")
    void deveCalcularValorEstimadoCorretamente() {
        // Arrange - 500 pontos = R$ 50,00
        cliente.setPontos(500);
        when(usuarioRepository.findByEmail("cliente@raizes.com"))
                .thenReturn(Optional.of(cliente));

        // Act
        SaldoPontosResponseDTO response = fidelidadeService.consultarSaldo("cliente@raizes.com");

        // Assert
        assertEquals(500, response.getSaldoPontos());
        assertEquals(50.0, response.getValorEstimadoEmDesconto());
    }

    // ============================================
    // TESTES DE ERRO
    // ============================================

    @Test
    @DisplayName("Deve lancar excecao quando usuario nao e encontrado")
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        // Arrange
        when(usuarioRepository.findByEmail("inexistente@raizes.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> fidelidadeService.consultarSaldo("inexistente@raizes.com"));

        assertEquals("USUARIO_NAO_ENCONTRADO", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("nao encontrado"));

        verify(usuarioRepository).findByEmail("inexistente@raizes.com");
    }

    @Test
    @DisplayName("Deve registrar consentimento explicito com versao e data")
    void deveRegistrarConsentimentoExplicito() {
        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(consentimentoLgpdRepository.save(any(ConsentimentoLgpd.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConsentimentoResponseDTO response = fidelidadeService.concederConsentimento("cliente@raizes.com");

        assertTrue(response.getAtivo());
        assertEquals(FinalidadeConsentimento.FIDELIDADE, response.getFinalidade());
        assertTrue(response.getDescricaoFinalidade().contains("pontos"));
        assertEquals("1.0", response.getVersaoTermoAtual());
        assertEquals("1.0", response.getVersaoRegistrada());
        assertNotNull(response.getRegistradoEm());
        verify(consentimentoLgpdRepository).save(argThat(evento ->
                Boolean.TRUE.equals(evento.getConcedido())
                        && "API_AUTOSSERVICO".equals(evento.getOrigem())
                        && cliente.equals(evento.getUsuario())));
    }

    @Test
    @DisplayName("Deve considerar inativo quando ainda nao existe consentimento")
    void deveConsultarConsentimentoInexistenteComoInativo() {
        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(consentimentoLgpdRepository
                .findTopByUsuarioIdAndFinalidadeOrderByRegistradoEmDescIdDesc(1L, FinalidadeConsentimento.FIDELIDADE))
                .thenReturn(Optional.empty());

        ConsentimentoResponseDTO response = fidelidadeService.consultarConsentimento("cliente@raizes.com");

        assertFalse(response.getAtivo());
        assertNull(response.getVersaoRegistrada());
        assertNull(response.getRegistradoEm());
    }

    @Test
    @DisplayName("Deve revogar consentimento preservando evento de auditoria")
    void deveRevogarConsentimento() {
        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(consentimentoLgpdRepository.save(any(ConsentimentoLgpd.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConsentimentoResponseDTO response = fidelidadeService.revogarConsentimento("cliente@raizes.com");

        assertFalse(response.getAtivo());
        assertNotNull(response.getRegistradoEm());
        verify(consentimentoLgpdRepository).save(argThat(evento -> Boolean.FALSE.equals(evento.getConcedido())));
    }

    @Test
    @DisplayName("Deve bloquear resgate quando consentimento foi revogado")
    void deveBloquearResgateComConsentimentoRevogado() {
        ConsentimentoLgpd revogacao = criarEventoConsentimento(false, "1.0");
        when(consentimentoLgpdRepository
                .findTopByUsuarioIdAndFinalidadeOrderByRegistradoEmDescIdDesc(1L, FinalidadeConsentimento.FIDELIDADE))
                .thenReturn(Optional.of(revogacao));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> fidelidadeService.validarConsentimentoParaResgate(cliente));

        assertEquals("CONSENTIMENTO_FIDELIDADE_NECESSARIO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve bloquear resgate quando consentimento pertence a versao antiga")
    void deveBloquearResgateComVersaoAntiga() {
        ConsentimentoLgpd aceiteAntigo = criarEventoConsentimento(true, "0.9");
        when(consentimentoLgpdRepository
                .findTopByUsuarioIdAndFinalidadeOrderByRegistradoEmDescIdDesc(1L, FinalidadeConsentimento.FIDELIDADE))
                .thenReturn(Optional.of(aceiteAntigo));

        assertThrows(RegraNegocioException.class,
                () -> fidelidadeService.validarConsentimentoParaResgate(cliente));
    }

    @Test
    @DisplayName("Deve autorizar resgate com consentimento ativo na versao vigente")
    void deveAutorizarResgateComConsentimentoVigente() {
        ConsentimentoLgpd aceite = criarEventoConsentimento(true, "1.0");
        when(consentimentoLgpdRepository
                .findTopByUsuarioIdAndFinalidadeOrderByRegistradoEmDescIdDesc(1L, FinalidadeConsentimento.FIDELIDADE))
                .thenReturn(Optional.of(aceite));

        assertDoesNotThrow(() -> fidelidadeService.validarConsentimentoParaResgate(cliente));
    }

    private ConsentimentoLgpd criarEventoConsentimento(boolean concedido, String versao) {
        ConsentimentoLgpd evento = new ConsentimentoLgpd();
        evento.setUsuario(cliente);
        evento.setFinalidade(FinalidadeConsentimento.FIDELIDADE);
        evento.setVersaoTermo(versao);
        evento.setConcedido(concedido);
        evento.setRegistradoEm(LocalDateTime.now());
        evento.setOrigem("API_AUTOSSERVICO");
        return evento;
    }
}

package com.raizesdonordeste.backendapi.security;

import com.raizesdonordeste.backendapi.service.DocumentoLegalService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AceiteTermosUsoFilter - exigência de aceite vigente")
class AceiteTermosUsoFilterTest {

    @Mock
    private DocumentoLegalService documentoLegalService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve permitir operação autenticada quando o aceite está vigente")
    void devePermitirComAceiteVigente() throws Exception {
        autenticar();
        when(documentoLegalService.possuiAceiteVigente("cliente@raizes.com")).thenReturn(true);
        MockHttpServletRequest request = request("/api/pedidos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AceiteTermosUsoFilter(documentoLegalService).doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve bloquear operação autenticada quando falta o aceite vigente")
    void deveBloquearSemAceiteVigente() throws Exception {
        autenticar();
        when(documentoLegalService.possuiAceiteVigente("cliente@raizes.com")).thenReturn(false);
        MockHttpServletRequest request = request("/api/pedidos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AceiteTermosUsoFilter(documentoLegalService).doFilter(request, response, filterChain);

        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString().contains("TERMOS_USO_NAO_ACEITOS"));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Deve permitir consulta e renovação do aceite mesmo quando desatualizado")
    void devePermitirAcessoAosDocumentosLegais() throws Exception {
        autenticar();
        MockHttpServletRequest request = request("/api/documentos-legais/termos-uso/aceite");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AceiteTermosUsoFilter(documentoLegalService).doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(documentoLegalService);
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "cliente@raizes.com", null, java.util.List.of()));
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/api");
        request.setRequestURI(uri);
        return request;
    }
}

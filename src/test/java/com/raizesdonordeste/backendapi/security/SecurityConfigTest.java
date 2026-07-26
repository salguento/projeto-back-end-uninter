package com.raizesdonordeste.backendapi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig - Configuração CORS")
class SecurityConfigTest {

    @Mock
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AceiteTermosUsoFilter aceiteTermosUsoFilter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void configurar() {
        securityConfig = new SecurityConfig(authenticationEntryPoint, accessDeniedHandler, jwtTokenService,
                aceiteTermosUsoFilter);
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", new String[] { "https://app.exemplo.com" });
        ReflectionTestUtils.setField(securityConfig, "allowedMethods", new String[] { "GET", "POST" });
        ReflectionTestUtils.setField(securityConfig, "allowCredentials", true);
        ReflectionTestUtils.setField(securityConfig, "maxAge", 3600L);
    }

    @Test
    @DisplayName("Deve separar e normalizar cabeçalhos configurados por vírgula")
    void deveSepararENormalizarCabecalhosConfiguradosPorVirgula() {
        ReflectionTestUtils.setField(securityConfig, "allowedHeaders",
                "Authorization, Content-Type, Idempotency-Key, X-Request-ID");

        CorsConfiguration configuration = obterConfiguracao();

        assertEquals(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Request-ID"),
                configuration.getAllowedHeaders());
    }

    @Test
    @DisplayName("Deve preservar curinga como cabeçalho permitido")
    void devePreservarCuringaComoCabecalhoPermitido() {
        ReflectionTestUtils.setField(securityConfig, "allowedHeaders", "*");

        CorsConfiguration configuration = obterConfiguracao();

        assertEquals(List.of("*"), configuration.getAllowedHeaders());
    }

    private CorsConfiguration obterConfiguracao() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/teste");
        return source.getCorsConfiguration(request);
    }
}

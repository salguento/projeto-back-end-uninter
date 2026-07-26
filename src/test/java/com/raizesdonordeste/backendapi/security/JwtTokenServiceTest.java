package com.raizesdonordeste.backendapi.security;

import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.model.UsuarioUnidade;
import com.raizesdonordeste.backendapi.repository.UsuarioUnidadeRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenService - Testes de Geração e Validação de Tokens")
class JwtTokenServiceTest {

    @Mock
    private UsuarioUnidadeRepository usuarioUnidadeRepository;
    
    @InjectMocks
    private JwtTokenService jwtTokenService;
    
    private static final String SECRET_KEY = "minha-chave-secreta-super-segura-com-pelo-menos-32-caracteres-aqui";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenService, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenService, "expirationMinutes", 60L);
    }
    
    /**
     * Helper para configurar Authentication mockado de forma type-safe.
     */
    private Authentication mockAuthentication(String email, String... roles) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        
        // Usa doReturn para evitar warnings de raw type
        doReturn(authorities).when(authentication).getAuthorities();
        
        return authentication;
    }
    
    @Test
    @DisplayName("Deve gerar token JWT válido a partir de Authentication")
    void deveGerarTokenValido() {
        // Arrange
        Authentication authentication = mockAuthentication("cliente@raizes.com", "ROLE_CLIENTE");
        
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        
        UsuarioUnidade usuarioUnidade = mock(UsuarioUnidade.class);
        when(usuarioUnidade.getUnidade()).thenReturn(unidade);
        when(usuarioUnidadeRepository.findByUsuarioEmail("cliente@raizes.com"))
                .thenReturn(List.of(usuarioUnidade));
        
        // Act
        String token = jwtTokenService.gerarToken(authentication);
        
        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }
    
    @Test
    @DisplayName("Deve extrair email do token válido")
    void deveExtrairEmailDoToken() {
        // Arrange
        Authentication authentication = mockAuthentication("cliente@raizes.com", "ROLE_CLIENTE");
        when(usuarioUnidadeRepository.findByUsuarioEmail("cliente@raizes.com"))
                .thenReturn(List.of());
        
        String token = jwtTokenService.gerarToken(authentication);
        
        // Act
        String email = jwtTokenService.obterEmailDoToken(token);
        
        // Assert
        assertEquals("cliente@raizes.com", email);
    }
    
    @Test
    @DisplayName("Deve validar token e retornar Claims")
    void deveValidarTokenERetornarClaims() {
        // Arrange
        Authentication authentication = mockAuthentication("admin@raizes.com", "ROLE_ADMIN");
        when(usuarioUnidadeRepository.findByUsuarioEmail("admin@raizes.com"))
                .thenReturn(List.of());
        
        String token = jwtTokenService.gerarToken(authentication);
        
        // Act
        Claims claims = jwtTokenService.validarETraduzirToken(token);
        
        // Assert
        assertNotNull(claims);
        assertEquals("admin@raizes.com", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
    
    @Test
    @DisplayName("Deve extrair roles do Claims")
    void deveExtrairRolesDoClaims() {
        // Arrange
        Authentication authentication = mockAuthentication("gerente@raizes.com", "ROLE_GERENTE");
        when(usuarioUnidadeRepository.findByUsuarioEmail("gerente@raizes.com"))
                .thenReturn(List.of());
        
        String token = jwtTokenService.gerarToken(authentication);
        Claims claims = jwtTokenService.validarETraduzirToken(token);
        
        // Act
        List<String> roles = jwtTokenService.obterRoles(claims);
        
        // Assert
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertTrue(roles.contains("ROLE_GERENTE"));
    }
    
    @Test
    @DisplayName("Deve detectar token expirado")
    void deveDetectarTokenExpirado() {
        // Arrange - Claims mockado com data no passado
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 10000));
        
        // Act
        boolean expirado = jwtTokenService.isTokenExpirado(claims);
        
        // Assert
        assertTrue(expirado);
    }
    
    @Test
    @DisplayName("Deve detectar token não expirado")
    void deveDetectarTokenNaoExpirado() {
        // Arrange - Claims mockado com data no futuro
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 86400000));
        
        // Act
        boolean expirado = jwtTokenService.isTokenExpirado(claims);
        
        // Assert
        assertFalse(expirado);
    }
    
    @Test
    @DisplayName("Deve lançar exceção para token inválido")
    void deveLancarExcecaoParaTokenInvalido() {
        // Act & Assert
        assertThrows(Exception.class, 
                () -> jwtTokenService.validarETraduzirToken("token.invalido.aqui"));
    }
    
    @Test
    @DisplayName("Deve lançar exceção quando chave secreta é muito curta")
    void deveLancarExcecaoQuandoChaveCurta() {
        // Arrange
        ReflectionTestUtils.setField(jwtTokenService, "secret", "curta");
        
        Authentication authentication = mockAuthentication("cliente@raizes.com", "ROLE_CLIENTE");
        when(usuarioUnidadeRepository.findByUsuarioEmail(anyString())).thenReturn(List.of());
        
        // Act & Assert
        assertThrows(IllegalStateException.class, 
                () -> jwtTokenService.gerarToken(authentication));
    }

    @Test
    @DisplayName("Deve respeitar tempo de expiracao configurado")
    void deveRespeitarTempoDeExpiracaoConfigurado() {
        ReflectionTestUtils.setField(jwtTokenService, "expirationMinutes", 5L);
        Authentication authentication = mockAuthentication("cliente@raizes.com", "ROLE_CLIENTE");
        when(usuarioUnidadeRepository.findByUsuarioEmail("cliente@raizes.com")).thenReturn(List.of());

        Claims claims = jwtTokenService.validarETraduzirToken(jwtTokenService.gerarToken(authentication));
        long duracao = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertEquals(5 * 60_000L, duracao);
    }

    @Test
    @DisplayName("Deve rejeitar tempo de expiracao nao positivo")
    void deveRejeitarTempoDeExpiracaoNaoPositivo() {
        ReflectionTestUtils.setField(jwtTokenService, "expirationMinutes", 0L);
        Authentication authentication = mock(Authentication.class);

        assertThrows(IllegalStateException.class, () -> jwtTokenService.gerarToken(authentication));
        verifyNoInteractions(usuarioUnidadeRepository);
    }
}

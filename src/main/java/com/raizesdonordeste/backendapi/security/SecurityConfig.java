package com.raizesdonordeste.backendapi.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final int BCRYPT_STRENGTH = 12;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_GERENTE = "GERENTE";
    private static final String ROLE_CLIENTE = "CLIENTE";
    private static final String ROLE_ATENDENTE = "ATENDENTE";
    private static final String ROLE_COZINHA = "COZINHA";

    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/error",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    };

    private static final String[] PRODUTOS_PUBLICOS_GET = {
            "/produtos",
            "/produtos/{id}"
    };

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age}")
    private Long maxAge;

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtTokenService jwtTokenService;
    private final AceiteTermosUsoFilter aceiteTermosUsoFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        List<String> headersPermitidos = Arrays.stream(allowedHeaders.split(","))
                .map(String::trim)
                .filter(header -> !header.isEmpty())
                .toList();
        configuration.setAllowedHeaders(headersPermitidos);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);
        configuration.addExposedHeader("Authorization");
        configuration.addExposedHeader("X-Request-ID");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS configurado | Origens: {} | Metodos: {} | Cabecalhos: {} | Credenciais: {}",
                Arrays.toString(allowedOrigins), Arrays.toString(allowedMethods), headersPermitidos,
                allowCredentials);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Inicializando configuracao de seguranca");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(this::configurarAutorizacoes)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenService),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(aceiteTermosUsoFilter, JwtAuthenticationFilter.class);

        log.info("Configuracao de seguranca finalizada");

        return http.build();
    }

    private void configurarAutorizacoes(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        configurarRotasPublicas(auth);
        configurarDocumentosLegais(auth);
        configurarProdutos(auth);
        configurarUnidades(auth);
        configurarPedidos(auth);
        configurarUsuarios(auth);
        configurarEstoque(auth);
        configurarFidelidade(auth);
        configurarCampanhas(auth);
        configurarCupons(auth);

        auth.anyRequest().authenticated();
    }

    private void configurarDocumentosLegais(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.GET, "/documentos-legais/termos-uso").permitAll()
                .requestMatchers(HttpMethod.GET, "/documentos-legais/aviso-privacidade").permitAll()
                .requestMatchers(HttpMethod.GET, "/documentos-legais/termos-uso/aceite").authenticated()
                .requestMatchers(HttpMethod.PUT, "/documentos-legais/termos-uso/aceite").authenticated();
    }

    private void configurarRotasPublicas(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(PUBLIC_PATHS).permitAll();
    }

    private void configurarProdutos(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.GET, PRODUTOS_PUBLICOS_GET).permitAll()
                .requestMatchers(HttpMethod.POST, "/produtos").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.PUT, "/produtos/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.DELETE, "/produtos/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE);
    }

    private void configurarUnidades(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.GET, "/unidades").authenticated()
                .requestMatchers(HttpMethod.GET, "/unidades/{id}").authenticated()
                .requestMatchers(HttpMethod.POST, "/unidades").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PUT, "/unidades/{id}").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/unidades/{id}").hasRole(ROLE_ADMIN);
    }

    private void configurarPedidos(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.POST, "/pedidos").hasAnyRole(ROLE_CLIENTE, ROLE_ATENDENTE)
                .requestMatchers(HttpMethod.POST, "/pedidos/*/pagamento").hasAnyRole(ROLE_CLIENTE, ROLE_ATENDENTE)
                .requestMatchers(HttpMethod.POST, "/pedidos/*/cancelamento")
                .hasAnyRole(ROLE_CLIENTE, ROLE_ATENDENTE, ROLE_ADMIN)
                .requestMatchers(HttpMethod.GET, "/pedidos/*/pagamento/tentativas").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/pedidos/*/status")
                .hasAnyRole(ROLE_COZINHA, ROLE_ATENDENTE, ROLE_GERENTE, ROLE_ADMIN)
                .requestMatchers(HttpMethod.GET, "/pedidos").authenticated()
                .requestMatchers(HttpMethod.GET, "/pedidos/{id}").authenticated();
    }

    private void configurarUsuarios(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                .requestMatchers(HttpMethod.PUT, "/usuarios/{id}").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/usuarios/{id}").authenticated()
                .requestMatchers(HttpMethod.GET, "/usuarios").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.GET, "/usuarios/{id}").hasRole(ROLE_ADMIN);
    }

    private void configurarEstoque(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.POST, "/estoque").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.POST, "/estoque/*/movimentacoes").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.GET, "/estoque/**").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.PUT, "/estoque/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.DELETE, "/estoque/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE);
    }

    private void configurarFidelidade(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers("/fidelidade/**").authenticated();
    }

    private void configurarCampanhas(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.POST, "/campanhas").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.GET, "/campanhas").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.GET, "/campanhas/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.PUT, "/campanhas/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.DELETE, "/campanhas/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE);
    }

    private void configurarCupons(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.POST, "/cupons").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.GET, "/cupons").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.GET, "/cupons/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.PUT, "/cupons/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE)
                .requestMatchers(HttpMethod.DELETE, "/cupons/{id}").hasAnyRole(ROLE_ADMIN, ROLE_GERENTE);
    }
}

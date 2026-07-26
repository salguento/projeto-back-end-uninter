package com.raizesdonordeste.backendapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String PREFIXO_BEARER = "Bearer ";
    private static final int TAMANHO_PREFIXO_BEARER = 7;

    private static final String ERRO_TOKEN_INVALIDO = "TOKEN_INVALIDO";
    private static final String MENSAGEM_TOKEN_INVALIDO = "A assinatura do token JWT fornecido e invalida ou expirou.";
    private static final String CHARSET_UTF8 = "UTF-8";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Objects.requireNonNull(request, "Request nao pode ser nulo");
        Objects.requireNonNull(response, "Response nao pode ser nulo");
        Objects.requireNonNull(filterChain, "FilterChain nao pode ser nulo");

        String tokenHeader = request.getHeader(HEADER_AUTHORIZATION);

        if (isTokenPresente(tokenHeader)) {
            String token = tokenHeader.substring(TAMANHO_PREFIXO_BEARER);

            if (isTokenValido(token)) {
                try {
                    autenticarUsuario(token);
                } catch (JwtException | IllegalArgumentException e) {
                    log.warn("Token JWT invalido ou expirado: {}", e.getMessage());
                    escreverErro(response, request.getRequestURI(), ERRO_TOKEN_INVALIDO, MENSAGEM_TOKEN_INVALIDO);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenPresente(String tokenHeader) {
        return tokenHeader != null && tokenHeader.startsWith(PREFIXO_BEARER);
    }

    private boolean isTokenValido(String token) {
        return token != null && !token.trim().isEmpty();
    }

    private void autenticarUsuario(String token) {
        Claims claims = jwtTokenService.validarETraduzirToken(token);
        String email = jwtTokenService.obterSubject(claims);
        List<String> roles = jwtTokenService.obterRoles(claims);

        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Token JWT valido | Ator: {} | Roles: {}", LogPseudonymizer.id(email), roles);
    }

    private void escreverErro(HttpServletResponse response, String path, String error, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(CHARSET_UTF8);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                error,
                message,
                new ArrayList<>(),
                Instant.now(),
                path
        );

        OBJECT_MAPPER.writeValue(response.getOutputStream(), errorResponse);
    }
}

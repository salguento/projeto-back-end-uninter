package com.raizesdonordeste.backendapi.security;

import com.raizesdonordeste.backendapi.model.UsuarioUnidade;
import com.raizesdonordeste.backendapi.repository.UsuarioUnidadeRepository;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_UNIDADE_IDS = "unidadeIds";
    private static final long MILISSEGUNDOS_POR_MINUTO = 60000L;
    private static final int MINIMUM_SECRET_LENGTH = 32;
    private static final String SECRET_TOO_SHORT_MESSAGE = "Chave secreta JWT deve ter pelo menos 32 caracteres";

    private final UsuarioUnidadeRepository usuarioUnidadeRepository;

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-minutes:60}")
    private long expirationMinutes;

    private SecretKey signingKey;

    @PostConstruct
    void validarConfiguracao() {
        validarSecretKey();
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("Tempo de expiracao JWT deve ser maior que zero.");
        }
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validarETraduzirToken(String token) {
        Objects.requireNonNull(token, "Token nao pode ser nulo");

        if (token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token nao pode ser vazio");
        }

        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("Token JWT invalido: {}", e.getMessage());
            throw e;
        }
    }

    public String obterSubject(Claims claims) {
        Objects.requireNonNull(claims, "Claims nao podem ser nulos");
        return claims.getSubject();
    }

    public String obterEmailDoToken(String token) {
        Claims claims = validarETraduzirToken(token);
        return obterSubject(claims);
    }

    @SuppressWarnings("unchecked")
    public List<String> obterRoles(Claims claims) {
        Objects.requireNonNull(claims, "Claims nao podem ser nulos");
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        return roles != null ? roles : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<Long> obterUnidadeIds(Claims claims) {
        Objects.requireNonNull(claims, "Claims nao podem ser nulos");
        List<Long> unidadeIds = claims.get(CLAIM_UNIDADE_IDS, List.class);
        return unidadeIds != null ? unidadeIds : Collections.emptyList();
    }

    public boolean isTokenExpirado(Claims claims) {
        Objects.requireNonNull(claims, "Claims nao podem ser nulos");
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    public String gerarToken(Authentication authentication) {
        Objects.requireNonNull(authentication, "Authentication nao pode ser nula");
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("Tempo de expiracao JWT deve ser maior que zero.");
        }

        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMinutes * MILISSEGUNDOS_POR_MINUTO);

        String email = authentication.getName();
        List<Long> unidadeIds = buscarUnidadesDoUsuario(email);
        List<String> roles = extrairRoles(authentication);

        String token = Jwts.builder()
                .subject(email)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_UNIDADE_IDS, unidadeIds)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(getSigningKey())
                .compact();

        log.debug("Token JWT gerado | Ator: {} | Roles: {} | Unidades: {}",
                LogPseudonymizer.id(email), roles, unidadeIds);

        return token;
    }

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            validarSecretKey();
            signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return signingKey;
    }

    private void validarSecretKey() {
        if (secret == null || secret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException(SECRET_TOO_SHORT_MESSAGE);
        }
    }

    private List<Long> buscarUnidadesDoUsuario(String email) {
        List<UsuarioUnidade> usuarioUnidades = usuarioUnidadeRepository.findByUsuarioEmail(email);
        return usuarioUnidades.stream()
                .map(uu -> uu.getUnidade().getId())
                .collect(Collectors.toList());
    }

    private List<String> extrairRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }
}

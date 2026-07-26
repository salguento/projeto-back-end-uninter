package com.raizesdonordeste.backendapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.DocumentoLegalService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class AceiteTermosUsoFilter extends OncePerRequestFilter {

    private static final String CAMINHO_DOCUMENTOS = "/documentos-legais/";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final DocumentoLegalService documentoLegalService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (deveValidar(request, authentication)
                && !documentoLegalService.possuiAceiteVigente(authentication.getName())) {
            escreverErro(response, request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean deveValidar(HttpServletRequest request, Authentication authentication) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                && !caminho.startsWith(CAMINHO_DOCUMENTOS);
    }

    private void escreverErro(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponseDTO error = new ErrorResponseDTO(
                DocumentoLegalService.TERMOS_USO_NAO_ACEITOS,
                DocumentoLegalService.MENSAGEM_TERMOS_NAO_ACEITOS,
                new ArrayList<>(), Instant.now(), path);
        OBJECT_MAPPER.writeValue(response.getOutputStream(), error);
    }
}

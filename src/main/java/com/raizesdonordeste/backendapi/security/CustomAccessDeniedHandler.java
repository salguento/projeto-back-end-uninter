package com.raizesdonordeste.backendapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final String ERRO_ACESSO_NEGADO = "ACESSO_NEGADO";
    private static final String MENSAGEM_ACESSO_NEGADO = "Voce nao tem permissao para acessar este recurso.";
    private static final String CHARSET_UTF8 = "UTF-8";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Objects.requireNonNull(request, "Request nao pode ser nulo");
        Objects.requireNonNull(response, "Response nao pode ser nulo");
        Objects.requireNonNull(accessDeniedException, "AccessDeniedException nao pode ser nula");

        log.warn("Acesso negado | URI: {} | Ator: {} | Motivo: {}",
                request.getRequestURI(),
                LogPseudonymizer.id(request.getUserPrincipal() != null
                        ? request.getUserPrincipal().getName() : null),
                accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(CHARSET_UTF8);

        ErrorResponseDTO error = new ErrorResponseDTO(
                ERRO_ACESSO_NEGADO,
                MENSAGEM_ACESSO_NEGADO,
                new ArrayList<>(),
                Instant.now(),
                request.getRequestURI()
        );

        OBJECT_MAPPER.writeValue(response.getOutputStream(), error);
    }
}

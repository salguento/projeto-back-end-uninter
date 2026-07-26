package com.raizesdonordeste.backendapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String ERRO_NAO_AUTENTICADO = "NAO_AUTENTICADO";
    private static final String MENSAGEM_NAO_AUTENTICADO = "Autenticacao necessaria para acessar este recurso.";
    private static final String CHARSET_UTF8 = "UTF-8";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Objects.requireNonNull(request, "Request nao pode ser nulo");
        Objects.requireNonNull(response, "Response nao pode ser nulo");
        Objects.requireNonNull(authException, "AuthenticationException nao pode ser nula");

        log.warn("Acesso nao autenticado | URI: {} | Motivo: {}",
                request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(CHARSET_UTF8);

        ErrorResponseDTO error = new ErrorResponseDTO(
                ERRO_NAO_AUTENTICADO,
                MENSAGEM_NAO_AUTENTICADO,
                new ArrayList<>(),
                Instant.now(),
                request.getRequestURI()
        );

        OBJECT_MAPPER.writeValue(response.getOutputStream(), error);
    }
}
package com.raizesdonordeste.backendapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String MDC_REQUEST_ID = "requestId";
    private static final Pattern REQUEST_ID_VALIDO = Pattern.compile("[A-Za-z0-9._:-]{8,100}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = normalizarOuGerar(request.getHeader(HEADER_REQUEST_ID));
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private String normalizarOuGerar(String recebido) {
        if (recebido != null) {
            String normalizado = recebido.trim();
            if (REQUEST_ID_VALIDO.matcher(normalizado).matches()) {
                return normalizado;
            }
        }
        return UUID.randomUUID().toString();
    }
}

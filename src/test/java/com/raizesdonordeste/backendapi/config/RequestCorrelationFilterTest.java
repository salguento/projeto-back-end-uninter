package com.raizesdonordeste.backendapi.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void deveGerarRequestIdQuandoHeaderNaoFoiInformado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(response.getHeader(RequestCorrelationFilter.HEADER_REQUEST_ID));
        assertFalse(response.getHeader(RequestCorrelationFilter.HEADER_REQUEST_ID).isBlank());
        assertNull(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
    }

    @Test
    void devePropagarRequestIdValidoRecebido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER_REQUEST_ID, "postman-request-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> requestIdDuranteFiltro = new AtomicReference<>();
        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> requestIdDuranteFiltro.set(
                        MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID)));

        assertEquals("postman-request-001", response.getHeader(RequestCorrelationFilter.HEADER_REQUEST_ID));
        assertEquals("postman-request-001", requestIdDuranteFiltro.get());
        assertNull(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
    }

    @Test
    void deveSubstituirRequestIdInvalidoParaEvitarInjecaoEmLog() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER_REQUEST_ID, "id\nlog-forjado");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestCorrelationFilter.HEADER_REQUEST_ID);
        assertNotEquals("id\nlog-forjado", requestId);
        assertFalse(requestId.contains("\n"));
    }
}

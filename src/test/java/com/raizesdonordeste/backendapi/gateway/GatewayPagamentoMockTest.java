package com.raizesdonordeste.backendapi.gateway;

import com.raizesdonordeste.backendapi.exception.GatewayPagamentoIndisponivelException;
import com.raizesdonordeste.backendapi.model.FormaPagamento;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayPagamentoMock - Simulacao e indisponibilidade")
class GatewayPagamentoMockTest {

    @Mock
    private HttpServletRequest request;

    private GatewayPagamentoMock gateway;

    @BeforeEach
    void setUp() {
        gateway = new GatewayPagamentoMock(request);
        ReflectionTestUtils.setField(gateway, "permitirStatusForcado", true);
        ReflectionTestUtils.setField(gateway, "forcarStatus", "");
        ReflectionTestUtils.setField(gateway, "forcarStatusEstorno", "");
        ReflectionTestUtils.setField(gateway, "taxaAprovacao", 0.8);
    }

    @Test
    @DisplayName("Deve retornar aprovado quando header de desenvolvimento solicitar")
    void deveForcarAprovacaoPorHeader() {
        when(request.getHeader(GatewayPagamentoMock.HEADER_FORCAR_STATUS)).thenReturn("APROVADO");

        ResultadoGatewayPagamento resultado = gateway.processar(FormaPagamento.PIX);

        assertEquals(ResultadoGatewayPagamento.APROVADO, resultado);
    }

    @Test
    @DisplayName("Deve retornar recusado quando propriedade solicitar")
    void deveForcarRecusaPorPropriedade() {
        ReflectionTestUtils.setField(gateway, "forcarStatus", "RECUSADO");

        ResultadoGatewayPagamento resultado = gateway.processar(FormaPagamento.CARTAO);

        assertEquals(ResultadoGatewayPagamento.RECUSADO, resultado);
    }

    @Test
    @DisplayName("Deve representar indisponibilidade tecnica sem converte-la em recusa")
    void deveSimularIndisponibilidade() {
        when(request.getHeader(GatewayPagamentoMock.HEADER_FORCAR_STATUS)).thenReturn("INDISPONIVEL");

        GatewayPagamentoIndisponivelException exception = assertThrows(
                GatewayPagamentoIndisponivelException.class,
                () -> gateway.processar(FormaPagamento.PIX));

        assertEquals(GatewayPagamentoIndisponivelException.MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("Deve retornar confirmacao ao processar estorno")
    void deveConfirmarEstorno() {
        when(request.getHeader(GatewayPagamentoMock.HEADER_FORCAR_STATUS_ESTORNO))
                .thenReturn("CONFIRMADO");

        ResultadoGatewayEstorno resultado = gateway.estornar("TXN-12345678");

        assertTrue(resultado.codigoConfirmacao().matches("EST-[A-Z0-9]{8}"));
    }

    @Test
    @DisplayName("Deve representar indisponibilidade tecnica durante estorno")
    void deveSimularIndisponibilidadeDuranteEstorno() {
        when(request.getHeader(GatewayPagamentoMock.HEADER_FORCAR_STATUS_ESTORNO))
                .thenReturn("INDISPONIVEL");

        GatewayPagamentoIndisponivelException exception = assertThrows(
                GatewayPagamentoIndisponivelException.class,
                () -> gateway.estornar("TXN-12345678"));

        assertEquals(GatewayPagamentoIndisponivelException.MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("Deve ignorar header quando simulacao por requisicao estiver desabilitada")
    void deveIgnorarHeaderQuandoDesabilitado() {
        ReflectionTestUtils.setField(gateway, "permitirStatusForcado", false);
        ReflectionTestUtils.setField(gateway, "forcarStatus", "RECUSADO");

        ResultadoGatewayPagamento resultado = gateway.processar(FormaPagamento.PIX);

        assertEquals(ResultadoGatewayPagamento.RECUSADO, resultado);
    }

    @Test
    @DisplayName("Deve ignorar status desconhecido e usar resultado simulado")
    void deveIgnorarStatusDesconhecido() {
        GatewayPagamentoMock gatewayControlado = new GatewayPagamentoMock(request) {
            @Override
            protected ResultadoGatewayPagamento simularResultado() {
                return ResultadoGatewayPagamento.APROVADO;
            }
        };
        ReflectionTestUtils.setField(gatewayControlado, "permitirStatusForcado", true);
        ReflectionTestUtils.setField(gatewayControlado, "forcarStatus", "");
        when(request.getHeader(GatewayPagamentoMock.HEADER_FORCAR_STATUS)).thenReturn("ERRO_DESCONHECIDO");

        ResultadoGatewayPagamento resultado = gatewayControlado.processar(FormaPagamento.DINHEIRO);

        assertEquals(ResultadoGatewayPagamento.APROVADO, resultado);
    }
}

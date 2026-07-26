package com.raizesdonordeste.backendapi.gateway;

import com.raizesdonordeste.backendapi.exception.GatewayPagamentoIndisponivelException;
import com.raizesdonordeste.backendapi.model.FormaPagamento;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayPagamentoMock implements GatewayPagamento {

    public static final String HEADER_FORCAR_STATUS = "X-Forcar-Status-Pagamento";
    public static final String HEADER_FORCAR_STATUS_ESTORNO = "X-Forcar-Status-Estorno";
    private static final String STATUS_INDISPONIVEL = "INDISPONIVEL";
    private static final String STATUS_ESTORNO_CONFIRMADO = "CONFIRMADO";
    private static final String PREFIXO_ESTORNO = "EST-";

    @Value("${app.pagamento.forcar-status:}")
    private String forcarStatus;

    @Value("${app.pagamento.forcar-status-estorno:}")
    private String forcarStatusEstorno;

    @Value("${app.pagamento.taxa-aprovacao:0.8}")
    private double taxaAprovacao = 0.8;

    @Value("${app.pagamento.permitir-status-forcado:false}")
    private boolean permitirStatusForcado;

    private final HttpServletRequest httpServletRequest;

    @Override
    public ResultadoGatewayPagamento processar(FormaPagamento formaPagamento) {
        String statusForcado = obterStatusForcado();
        if (STATUS_INDISPONIVEL.equals(statusForcado)) {
            log.warn("Gateway de pagamento simulado indisponivel");
            throw new GatewayPagamentoIndisponivelException();
        }
        if (statusForcado != null) {
            return ResultadoGatewayPagamento.valueOf(statusForcado);
        }
        return simularResultado();
    }

    @Override
    public ResultadoGatewayEstorno estornar(String codigoTransacao) {
        Objects.requireNonNull(codigoTransacao, "Codigo da transacao nao pode ser nulo");
        if (codigoTransacao.isBlank()) {
            throw new IllegalArgumentException("Codigo da transacao nao pode ser vazio");
        }

        String statusForcado = obterStatusEstornoForcado();
        if (STATUS_INDISPONIVEL.equals(statusForcado)) {
            log.warn("Gateway de pagamento simulado indisponivel para estorno | Transacao: {}",
                    codigoTransacao);
            throw new GatewayPagamentoIndisponivelException();
        }

        ResultadoGatewayEstorno resultado = simularEstorno();
        log.info("Estorno confirmado pelo gateway simulado | Transacao: {} | Confirmacao: {}",
                codigoTransacao, resultado.codigoConfirmacao());
        return resultado;
    }

    private String obterStatusForcado() {
        if (permitirStatusForcado) {
            String statusHeader = normalizarStatus(httpServletRequest.getHeader(HEADER_FORCAR_STATUS));
            if (statusHeader != null) {
                log.info("Status do gateway forcado via header | Status: {}", statusHeader);
                return statusHeader;
            }
        }

        String statusConfigurado = normalizarStatus(forcarStatus);
        if (statusConfigurado != null) {
            log.info("Status do gateway forcado via propriedade | Status: {}", statusConfigurado);
        }
        return statusConfigurado;
    }

    private String obterStatusEstornoForcado() {
        if (permitirStatusForcado) {
            String statusHeader = normalizarStatusEstorno(
                    httpServletRequest.getHeader(HEADER_FORCAR_STATUS_ESTORNO));
            if (statusHeader != null) {
                log.info("Status do estorno forcado via header | Status: {}", statusHeader);
                return statusHeader;
            }
        }

        String statusConfigurado = normalizarStatusEstorno(forcarStatusEstorno);
        if (statusConfigurado != null) {
            log.info("Status do estorno forcado via propriedade | Status: {}", statusConfigurado);
        }
        return statusConfigurado;
    }

    private String normalizarStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalizado = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case "APROVADO", "RECUSADO", STATUS_INDISPONIVEL -> normalizado;
            default -> null;
        };
    }

    private String normalizarStatusEstorno(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalizado = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalizado) {
            case STATUS_ESTORNO_CONFIRMADO, STATUS_INDISPONIVEL -> normalizado;
            default -> null;
        };
    }

    protected ResultadoGatewayPagamento simularResultado() {
        return Math.random() < taxaAprovacao
                ? ResultadoGatewayPagamento.APROVADO
                : ResultadoGatewayPagamento.RECUSADO;
    }

    protected ResultadoGatewayEstorno simularEstorno() {
        return new ResultadoGatewayEstorno(PREFIXO_ESTORNO
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
    }
}

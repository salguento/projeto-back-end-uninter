package com.raizesdonordeste.backendapi.exception;

public class GatewayPagamentoIndisponivelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String ERROR_CODE = "GATEWAY_PAGAMENTO_INDISPONIVEL";
    public static final String MESSAGE = "O servico de pagamento esta temporariamente indisponivel. Tente novamente.";

    public GatewayPagamentoIndisponivelException() {
        super(MESSAGE);
    }

    public GatewayPagamentoIndisponivelException(Throwable cause) {
        super(MESSAGE, cause);
    }
}

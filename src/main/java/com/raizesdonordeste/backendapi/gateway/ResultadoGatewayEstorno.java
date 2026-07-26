package com.raizesdonordeste.backendapi.gateway;

import java.util.Objects;

public record ResultadoGatewayEstorno(String codigoConfirmacao) {

    public ResultadoGatewayEstorno {
        Objects.requireNonNull(codigoConfirmacao, "Codigo de confirmacao do estorno nao pode ser nulo");
        if (codigoConfirmacao.isBlank()) {
            throw new IllegalArgumentException("Codigo de confirmacao do estorno nao pode ser vazio");
        }
    }
}

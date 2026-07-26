package com.raizesdonordeste.backendapi.exception;

public class ReservaExpiradaException extends RegraNegocioException {
    public ReservaExpiradaException(String message) {
        super("RESERVA_EXPIRADA", message);
    }
}

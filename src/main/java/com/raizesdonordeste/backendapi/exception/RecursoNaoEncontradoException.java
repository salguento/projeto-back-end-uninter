package com.raizesdonordeste.backendapi.exception;

import java.util.Objects;

public class RecursoNaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public RecursoNaoEncontradoException(String errorCode, String message) {
        super(Objects.requireNonNull(message, "Mensagem da excecao nao pode ser nula"));
        this.errorCode = Objects.requireNonNull(errorCode, "Codigo de erro nao pode ser nulo");

        if (errorCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Codigo de erro nao pode ser vazio");
        }
    }

    public RecursoNaoEncontradoException(String errorCode, String message, Throwable cause) {
        super(Objects.requireNonNull(message, "Mensagem da excecao nao pode ser nula"),
              Objects.requireNonNull(cause, "Causa da excecao nao pode ser nula"));
        this.errorCode = Objects.requireNonNull(errorCode, "Codigo de erro nao pode ser nulo");

        if (errorCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Codigo de erro nao pode ser vazio");
        }
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("RecursoNaoEncontradoException{errorCode='%s', message='%s'}",
                errorCode, getMessage());
    }
}
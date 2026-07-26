package com.raizesdonordeste.backendapi.dto;

import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Formato padronizado de erro da API",
        example = "{\"error\":\"VALIDACAO\",\"message\":\"Dados inválidos.\",\"details\":[{\"field\":\"itens\",\"issue\":\"O pedido deve conter pelo menos um item.\"}],\"timestamp\":\"2026-07-20T14:30:00Z\",\"path\":\"/api/pedidos\",\"requestId\":\"7d9a35ab-74af-4fac-b21e-b48a68d9613e\"}")
public record ErrorResponseDTO(
    @Schema(example = "VALIDATION_ERROR") String error,
    @Schema(example = "Dados inválidos.") String message,
    @Schema(description = "Erros específicos por campo") List<ErrorDetail> details,
    @Schema(example = "2026-07-13T14:30:00Z") Instant timestamp,
    @Schema(example = "/api/pedidos") String path,
    @Schema(description = "Identificador para correlação em logs", example = "7d9a35ab-74af-4fac-b21e-b48a68d9613e") String requestId
) {
    public ErrorResponseDTO(String error, String message, List<ErrorDetail> details, Instant timestamp, String path) {
        this(error, message, details, timestamp, path, MDC.get("requestId"));
    }

    public record ErrorDetail(
            @Schema(example = "itens") String field,
            @Schema(example = "O pedido deve conter pelo menos um item.") String issue) {}
}

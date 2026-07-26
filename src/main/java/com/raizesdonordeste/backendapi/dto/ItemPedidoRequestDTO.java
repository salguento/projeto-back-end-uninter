package com.raizesdonordeste.backendapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ItemPedidoRequestDTO {

    @NotNull(message = "O ID do produto é obrigatório.")
    @Schema(description = "Produto solicitado", example = "1")
    private Long produtoId;

    @NotNull(message = "A quantidade é obrigatória.")
    @Min(value = 1, message = "A quantidade mínima por produto deve ser 1.")
    @Schema(description = "Quantidade solicitada", example = "2", minimum = "1")
    private Integer quantidade;
}

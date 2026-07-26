package com.raizesdonordeste.backendapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Quantidade de um produto em uma unidade")
public class EstoqueDTO {
    
    @NotNull(message = "ID do produto é obrigatório.")
    @Schema(example = "1")
    private Long produtoId;
    
    @NotNull(message = "ID da unidade é obrigatório.")
    @Schema(example = "1")
    private Long unidadeId;
    
    @NotNull(message = "Quantidade é obrigatória.")
    @Min(value = 0, message = "Quantidade não pode ser negativa.")
    @Schema(example = "50", minimum = "0")
    private Integer quantidade;
}

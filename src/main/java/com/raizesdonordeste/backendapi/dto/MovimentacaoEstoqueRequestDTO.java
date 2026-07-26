package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.TipoMovimentacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Entrada ou saída manual de estoque")
public class MovimentacaoEstoqueRequestDTO {

    @NotNull(message = "O tipo da movimentação é obrigatório.")
    @Schema(example = "ENTRADA")
    private TipoMovimentacaoEstoque tipo;

    @NotNull(message = "A quantidade é obrigatória.")
    @Min(value = 1, message = "A quantidade deve ser maior que zero.")
    @Schema(example = "10", minimum = "1")
    private Integer quantidade;

    @NotBlank(message = "O motivo é obrigatório.")
    @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres.")
    @Schema(example = "Recebimento do fornecedor")
    private String motivo;
}

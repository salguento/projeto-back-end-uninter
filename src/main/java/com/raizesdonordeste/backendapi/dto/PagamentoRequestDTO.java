package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class PagamentoRequestDTO {
    @NotNull(message = "Forma de pagamento é obrigatória")
    @Schema(description = "Forma utilizada nesta tentativa", example = "PIX")
    private FormaPagamento formaPagamento;
}

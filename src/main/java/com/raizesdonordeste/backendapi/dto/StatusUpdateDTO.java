package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.StatusPedido;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class StatusUpdateDTO {
    @NotNull(message = "O status é obrigatório")
    @Schema(description = "Próximo estado permitido pela máquina de estados", example = "EM_PREPARACAO")
    private StatusPedido status;
}

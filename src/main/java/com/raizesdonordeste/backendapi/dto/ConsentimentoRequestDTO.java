package com.raizesdonordeste.backendapi.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class ConsentimentoRequestDTO {

    @NotNull(message = "A manifestação de consentimento é obrigatória.")
    @AssertTrue(message = "O consentimento deve ser aceito explicitamente.")
    @Schema(description = "Manifestação livre, informada e inequívoca do titular", example = "true", allowableValues = "true")
    private Boolean aceito;
}

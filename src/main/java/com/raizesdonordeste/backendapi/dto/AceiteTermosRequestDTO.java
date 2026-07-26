package com.raizesdonordeste.backendapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Aceite da versão vigente dos termos de uso")
public class AceiteTermosRequestDTO {

    @NotNull(message = "A decisão sobre os termos de uso é obrigatória.")
    @AssertTrue(message = "Os termos de uso devem ser aceitos explicitamente.")
    @Schema(example = "true", allowableValues = "true")
    private Boolean aceito;

    @NotBlank(message = "A versão dos termos de uso é obrigatória.")
    @Schema(example = "1.0")
    private String versao;

    @NotBlank(message = "O hash dos termos de uso é obrigatório.")
    @Size(min = 64, max = 64, message = "O hash dos termos de uso deve possuir 64 caracteres.")
    @Schema(example = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
    private String hashSha256;
}

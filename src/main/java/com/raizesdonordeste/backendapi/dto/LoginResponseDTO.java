package com.raizesdonordeste.backendapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
        description = "Token emitido após autenticação",
        example = "{\"token\":\"eyJhbGciOiJIUzI1NiJ9...\",\"tipo\":\"Bearer\"}")
public class LoginResponseDTO {
    @Schema(description = "JWT utilizado no cabeçalho Authorization", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;
    @Schema(description = "Tipo do token", example = "Bearer")
    private String tipo;
}

package com.raizesdonordeste.backendapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Credenciais para autenticação")
public class LoginRequestDTO {
    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "E-mail deve ser válido.")
    @Schema(description = "E-mail cadastrado", example = "cliente@exemplo.com")
    private String email;
    
    @NotBlank(message = "Senha é obrigatória.")
    @Schema(description = "Senha do usuário", example = "Senha@123", format = "password")
    private String senha;
}

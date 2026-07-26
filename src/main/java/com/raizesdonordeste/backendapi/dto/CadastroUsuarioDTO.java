package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro público de cliente e aceite dos termos de uso")
public class CadastroUsuarioDTO {

    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres.")
    @Schema(example = "Maria da Silva")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "E-mail deve ser válido.")
    @Schema(example = "maria@exemplo.com")
    private String email;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
    @Schema(example = "Senha@123", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String senha;

    @Schema(description = "Opcional; somente CLIENTE é aceito no cadastro público", example = "CLIENTE")
    private Perfil perfil;

    @Schema(example = "81999999999")
    private String telefone;

    @Schema(example = "12345678909")
    private String cpf;

    @NotNull(message = "A decisão sobre os termos de uso é obrigatória.")
    @AssertTrue(message = "Os termos de uso devem ser aceitos explicitamente.")
    @Schema(description = "Aceite contratual explícito dos termos de uso", example = "true", allowableValues = "true")
    private Boolean aceiteTermosUso;

    @NotBlank(message = "A versão dos termos de uso é obrigatória.")
    @Schema(description = "Versão obtida em GET /documentos-legais/termos-uso", example = "1.0")
    private String versaoTermosUso;

    @NotBlank(message = "O hash dos termos de uso é obrigatório.")
    @Size(min = 64, max = 64, message = "O hash dos termos de uso deve possuir 64 caracteres.")
    @Schema(description = "Hash SHA-256 obtido em GET /documentos-legais/termos-uso", example = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
    private String hashTermosUso;
}

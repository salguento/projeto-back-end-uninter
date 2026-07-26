package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Dados de cadastro ou atualização do usuário")
public class UsuarioDTO {
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

    @Schema(description = "Opcional; CLIENTE por padrão. Perfis privilegiados exigem autorização.", example = "CLIENTE")
    private Perfil perfil;

    @Schema(example = "81999999999")
    private String telefone;
    @Schema(example = "12345678909")
    private String cpf;
}

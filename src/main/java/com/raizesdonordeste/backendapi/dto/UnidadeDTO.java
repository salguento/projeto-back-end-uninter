package com.raizesdonordeste.backendapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Dados de uma unidade física")
public class UnidadeDTO {

    @NotBlank(message = "Nome e obrigatorio")
    @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
    @Schema(example = "Unidade Recife Centro")
    private String nome;

    @NotBlank(message = "Endereco e obrigatorio")
    @Size(max = 200, message = "Endereco deve ter no maximo 200 caracteres")
    @Schema(example = "Rua do Sol, 100")
    private String endereco;

    @NotBlank(message = "Cidade e obrigatoria")
    @Size(max = 100, message = "Cidade deve ter no maximo 100 caracteres")
    @Schema(example = "Recife")
    private String cidade;

    @NotBlank(message = "Estado e obrigatorio")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ter 2 letras maiusculas")
    @Schema(example = "PE")
    private String estado;

    @NotBlank(message = "CEP e obrigatorio")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP deve estar no formato 00000-000")
    @Schema(example = "50000-000")
    private String cep;

    @NotBlank(message = "Telefone e obrigatorio")
    @Size(min = 10, max = 15, message = "Telefone deve ter entre 10 e 15 digitos")
    @Schema(example = "8133334444")
    private String telefone;
}

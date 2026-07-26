package com.raizesdonordeste.backendapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Dados de um produto")
public class ProdutoDTO {
    
    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres.")
    @Schema(example = "Bolo de rolo")
    private String nome;
    
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
    @Schema(example = "Fatia artesanal de bolo de rolo")
    private String descricao;
    
    @NotNull(message = "Preço vigente é obrigatório.")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero.")
    @Schema(example = "12.90", minimum = "0.01")
    private BigDecimal precoVigente;

    @Schema(example = "true", defaultValue = "true")
    private Boolean ativo = true;
}

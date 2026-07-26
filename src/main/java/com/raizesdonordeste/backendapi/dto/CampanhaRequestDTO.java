package com.raizesdonordeste.backendapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Dados de uma campanha promocional")
public class CampanhaRequestDTO {
    
    @NotBlank(message = "Nome da campanha é obrigatório.")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres.")
    @Schema(example = "São João 2026")
    private String nome;
    
    @NotNull(message = "ID do produto é obrigatório.")
    @Schema(example = "1")
    private Long produtoId;
    
    @NotNull(message = "ID da unidade é obrigatório.")
    @Schema(example = "1")
    private Long unidadeId;
    
    @NotNull(message = "Preço promocional é obrigatório.")
    @DecimalMin(value = "0.01", message = "Preço promocional deve ser maior que zero.")
    @Schema(example = "9.90", minimum = "0.01")
    private BigDecimal precoPromocional;
    
    @NotNull(message = "Data de início é obrigatória.")
    @Schema(example = "2026-06-01T00:00:00")
    private LocalDateTime dataInicio;
    
    @NotNull(message = "Data de fim é obrigatória.")
    @Schema(example = "2026-06-30T23:59:59")
    private LocalDateTime dataFim;

    @Schema(example = "true", defaultValue = "true")
    private Boolean ativa = true;
}

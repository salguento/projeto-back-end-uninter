package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.TipoDesconto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Dados de um cupom de desconto")
public class CupomRequestDTO {
    
    @NotBlank(message = "Código do cupom é obrigatório.")
    @Size(min = 3, max = 50, message = "Código deve ter entre 3 e 50 caracteres.")
    @Schema(example = "BEMVINDO10")
    private String codigo;
    
    @NotNull(message = "Tipo de desconto é obrigatório.")
    @Schema(example = "PERCENTUAL")
    private TipoDesconto tipoDesconto;
    
    @NotNull(message = "Valor é obrigatório.")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero.")
    @Schema(description = "Percentual ou valor fixo, conforme o tipo", example = "10.00", minimum = "0.01")
    private BigDecimal valor;

    @Schema(example = "30.00", defaultValue = "0")
    private BigDecimal valorMinimoPedido = BigDecimal.ZERO;

    @Schema(description = "Unidade específica; nulo torna o cupom global", example = "1", nullable = true)
    private Long unidadeId;
    
    @NotNull(message = "Data de início é obrigatória.")
    @Schema(example = "2026-07-01T00:00:00")
    private LocalDateTime dataInicio;
    
    @NotNull(message = "Data de fim é obrigatória.")
    @Schema(example = "2026-07-31T23:59:59")
    private LocalDateTime dataFim;

    @Schema(description = "Limite total de usos; nulo significa ilimitado", example = "100", nullable = true)
    private Integer usoMaximo;

    @Schema(example = "true", defaultValue = "true")
    private Boolean ativo = true;
}

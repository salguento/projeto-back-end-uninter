package com.raizesdonordeste.backendapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(
        description = "Campanha promocional cadastrada",
        example = "{\"id\":1,\"nome\":\"Festival da Tapioca\",\"produtoId\":1,\"nomeProduto\":\"Tapioca de carne seca\",\"unidadeId\":1,\"nomeUnidade\":\"Unidade Centro\",\"precoPromocional\":9.90,\"dataInicio\":\"2026-07-01T00:00:00\",\"dataFim\":\"2026-07-31T23:59:59\",\"ativa\":true}")
public class CampanhaResponseDTO {
    private Long id;
    private String nome;
    private Long produtoId;
    private String nomeProduto;
    private Long unidadeId;
    private String nomeUnidade;
    private BigDecimal precoPromocional;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Boolean ativa;
}

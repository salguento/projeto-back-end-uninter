package com.raizesdonordeste.backendapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
        description = "Posição de estoque de um produto em uma unidade",
        example = "{\"id\":1,\"produtoId\":1,\"nomeProduto\":\"Tapioca de carne seca\",\"unidadeId\":1,\"nomeUnidade\":\"Unidade Centro\",\"quantidade\":25}")
public class EstoqueResponseDTO {
    private Long id;
    private Long produtoId;
    private String nomeProduto;
    private Long unidadeId;
    private String nomeUnidade;
    private Integer quantidade;
}

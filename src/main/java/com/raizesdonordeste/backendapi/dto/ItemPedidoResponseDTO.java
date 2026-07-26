package com.raizesdonordeste.backendapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(
        description = "Item e preço efetivamente utilizados no pedido",
        example = "{\"produtoId\":1,\"nomeProduto\":\"Tapioca de carne seca\",\"quantidade\":2,\"precoUnitario\":9.90}")
public class ItemPedidoResponseDTO {
    private Long produtoId;
    private String nomeProduto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
}

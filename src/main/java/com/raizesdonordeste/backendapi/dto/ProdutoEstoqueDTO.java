package com.raizesdonordeste.backendapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "Produto ativo com disponibilidade em uma unidade",
        example = "{\"produtoId\":1,\"nome\":\"Tapioca de carne seca\",\"descricao\":\"Tapioca recheada com carne seca e queijo coalho\",\"precoVigente\":25.00,\"estoqueDisponivel\":25,\"ativo\":true}")
public class ProdutoEstoqueDTO {
    private Long produtoId;
    private String nome;
    private String descricao;
    private BigDecimal precoVigente;
    private Integer estoqueDisponivel;
    private Boolean ativo;
}

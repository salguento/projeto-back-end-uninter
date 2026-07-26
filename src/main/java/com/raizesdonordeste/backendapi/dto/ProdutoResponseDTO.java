package com.raizesdonordeste.backendapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(
        description = "Produto do catálogo compartilhado",
        example = "{\"id\":1,\"nome\":\"Tapioca de carne seca\",\"descricao\":\"Tapioca recheada com carne seca e queijo coalho\",\"precoVigente\":25.00,\"ativo\":true}")
public class ProdutoResponseDTO {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal precoVigente;
    private Boolean ativo;
}

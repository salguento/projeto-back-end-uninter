package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.TipoDesconto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(
        description = "Cupom de desconto cadastrado",
        example = "{\"id\":1,\"codigo\":\"NORDESTE10\",\"tipoDesconto\":\"PERCENTUAL\",\"valor\":10.00,\"valorMinimoPedido\":30.00,\"unidadeId\":1,\"nomeUnidade\":\"Unidade Centro\",\"dataInicio\":\"2026-07-01T00:00:00\",\"dataFim\":\"2026-07-31T23:59:59\",\"usoMaximo\":100,\"usoAtual\":4,\"ativo\":true}")
public class CupomResponseDTO {
    private Long id;
    private String codigo;
    private TipoDesconto tipoDesconto;
    private BigDecimal valor;
    private BigDecimal valorMinimoPedido;
    private Long unidadeId;
    private String nomeUnidade;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Integer usoMaximo;
    private Integer usoAtual;
    private Boolean ativo;
}

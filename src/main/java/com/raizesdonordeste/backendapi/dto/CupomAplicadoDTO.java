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
        description = "Cupom efetivamente aplicado ao pedido",
        example = "{\"codigo\":\"NORDESTE10\",\"valorDesconto\":5.00,\"tipoDesconto\":\"PERCENTUAL\"}")
public class CupomAplicadoDTO {
    private String codigo;
    private BigDecimal valorDesconto;
    private String tipoDesconto;
}

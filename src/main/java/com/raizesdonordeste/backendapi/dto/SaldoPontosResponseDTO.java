package com.raizesdonordeste.backendapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
        description = "Saldo do programa de fidelidade",
        example = "{\"usuarioId\":1,\"email\":\"cliente@exemplo.com\",\"saldoPontos\":120,\"valorEstimadoEmDesconto\":12.0}")
public class SaldoPontosResponseDTO {
    @Schema(example = "1")
    private Long usuarioId;

    @Schema(example = "cliente@exemplo.com")
    private String email;

    @Schema(description = "Saldo contabil registrado; valores negativos representam pontos devidos após compensacao",
            example = "-15")
    private Integer saldoPontos;

    @Schema(description = "Valor atualmente disponivel para desconto; permanece zero enquanto houver saldo devedor",
            example = "0.0", minimum = "0")
    private Double valorEstimadoEmDesconto;
}

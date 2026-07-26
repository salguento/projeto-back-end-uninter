package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.FormaPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.raizesdonordeste.backendapi.model.StatusPedido;

@Data
@Schema(
        description = "Resultado de uma tentativa de pagamento simulado",
        example = "{\"pagamentoId\":1,\"pedidoId\":1,\"formaPagamento\":\"CARTAO\",\"valor\":42.80,\"status\":\"APROVADO\",\"codigoTransacao\":\"TX-20260720-0001\",\"codigoEstorno\":null,\"estornadoEm\":null,\"criadoEm\":\"2026-07-20T14:30:00\",\"statusPedido\":\"RECEBIDO\",\"tentativasRestantes\":2}")
public class PagamentoResponseDTO {
    private Long pagamentoId;
    private Long pedidoId;
    private FormaPagamento formaPagamento;
    private BigDecimal valor;
    private String status;
    private String codigoTransacao;
    private String codigoEstorno;
    private LocalDateTime estornadoEm;
    private LocalDateTime criadoEm;
    private StatusPedido statusPedido;
    private Integer tentativasRestantes;
}

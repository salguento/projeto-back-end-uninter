package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.CanalPedido;
import com.raizesdonordeste.backendapi.model.FormaPagamento;
import com.raizesdonordeste.backendapi.model.StatusPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(
        description = "Pedido com valores, itens, benefícios e estado atual",
        example = "{\"pedidoId\":1,\"status\":\"AGUARDANDO_PAGAMENTO\",\"canalPedido\":\"APP\",\"total\":42.80,\"desconto\":7.20,\"pontosUtilizados\":20,\"pontosConcedidos\":0,\"itens\":[{\"produtoId\":1,\"nomeProduto\":\"Tapioca de carne seca\",\"quantidade\":2,\"precoUnitario\":25.00}],\"formaPagamento\":\"CARTAO\",\"dataCriacao\":\"2026-07-20T14:30:00\",\"expiraEm\":\"2026-07-20T14:45:00\",\"cupomAplicado\":{\"codigo\":\"NORDESTE10\",\"valorDesconto\":5.00,\"tipoDesconto\":\"PERCENTUAL\"},\"usuarioRegistroId\":1,\"clienteId\":1,\"pedidoInterno\":false}")
public class PedidoResponseDTO {
    private Long pedidoId;
    private StatusPedido status;
    private CanalPedido canalPedido;
    private BigDecimal total;
    private BigDecimal desconto;
    private Integer pontosUtilizados;
    private Integer pontosConcedidos;
    private List<ItemPedidoResponseDTO> itens;
    private FormaPagamento formaPagamento;
    private LocalDateTime dataCriacao;
    private LocalDateTime expiraEm;
    private CupomAplicadoDTO cupomAplicado;
    private Long usuarioRegistroId;
    private Long clienteId;
    private Boolean pedidoInterno;
}

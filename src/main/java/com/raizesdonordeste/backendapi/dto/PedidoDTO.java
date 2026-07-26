package com.raizesdonordeste.backendapi.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;
import com.raizesdonordeste.backendapi.model.FormaPagamento;
import com.raizesdonordeste.backendapi.model.CanalPedido;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Dados para criação de um pedido")
public class PedidoDTO {
    @NotNull(message = "Unidade é obrigatória.")
    @Schema(description = "Unidade responsável pelo pedido", example = "1")
    private Long unidadeId;

    @NotNull(message = "O canal do pedido é obrigatório.")
    @Schema(description = "Canal de origem", example = "APP")
    private CanalPedido canalPedido;

    @NotEmpty(message = "O pedido deve conter pelo menos um item.")
    @Valid
    @Schema(description = "Itens solicitados")
    private List<ItemPedidoRequestDTO> itens;

    @NotNull(message = "Forma de pagamento é obrigatória.")
    @Schema(description = "Forma prevista para pagamento", example = "PIX")
    private FormaPagamento formaPagamento;
    
    @Schema(description = "Classificação de pedido interno explicitamente informada pelo atendente; não identifica, isoladamente, um pedido assistido", example = "false", defaultValue = "false")
    private Boolean pedidoInterno = false;

    @Schema(description = "Cliente destinatário informado pelo atendente em um pedido assistido; quando omitido, utiliza o usuário autenticado", example = "2", nullable = true)
    private Long clienteId;

    @Schema(description = "Solicita resgate dos pontos disponíveis; exige consentimento vigente", example = "false", defaultValue = "false")
    private Boolean usarPontos = false;

    @Schema(description = "Código de cupom opcional", example = "BEMVINDO10", nullable = true)
    private String codigoCupom;
}

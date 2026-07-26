package com.raizesdonordeste.backendapi.dto;

import java.time.LocalDateTime;

import com.raizesdonordeste.backendapi.model.TipoMovimentacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
        description = "Registro auditável de movimentação do estoque",
        example = "{\"id\":1,\"estoqueId\":1,\"produtoId\":1,\"unidadeId\":1,\"tipo\":\"ENTRADA\",\"quantidade\":10,\"saldoAnterior\":15,\"saldoPosterior\":25,\"motivo\":\"Reposição semanal\",\"ator\":\"usr_81cfaef08ecf\",\"criadoEm\":\"2026-07-20T14:30:00\"}")
public class MovimentacaoEstoqueResponseDTO {
    private Long id;
    private Long estoqueId;
    private Long produtoId;
    private Long unidadeId;
    private TipoMovimentacaoEstoque tipo;
    private Integer quantidade;
    private Integer saldoAnterior;
    private Integer saldoPosterior;
    private String motivo;
    @Schema(description = "Identificador pseudonimizado do responsável", example = "usr_81cfaef08ecf")
    private String ator;
    private LocalDateTime criadoEm;
}

package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.PagamentoRequestDTO;
import com.raizesdonordeste.backendapi.dto.PagamentoResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.PagamentoService;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/pedidos/{pedidoId}/pagamento")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Mock de pagamento, tentativas e estornos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    @Operation(summary = "Processar tentativa de pagamento", description = "Permissões: proprietário do pedido ou ATENDENTE da unidade. Máximo de três tentativas recusadas. Falhas técnicas do gateway não alteram o pedido nem consomem uma tentativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tentativa processada", content = @Content(schema = @Schema(implementation = PagamentoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem acesso ao pedido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Reserva expirada, limite atingido ou pagamento já aprovado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Forma de pagamento inválida", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "503", description = "Gateway de pagamento temporariamente indisponível; o pedido permanece aguardando pagamento", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PagamentoResponseDTO> processarPagamento(
            @PathVariable Long pedidoId,
            @Valid @RequestBody PagamentoRequestDTO dto,
            Principal principal) {
        Objects.requireNonNull(pedidoId, "PedidoId nao pode ser nulo");
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        Objects.requireNonNull(principal, "Principal nao pode ser nulo");
        Objects.requireNonNull(principal.getName(), "Nome do usuario nao pode ser nulo");
        
        log.info("POST /pedidos/{}/pagamento | Forma: {} | Ator: {}",
                pedidoId, dto.getFormaPagamento(), LogPseudonymizer.id(principal.getName()));
        PagamentoResponseDTO response = pagamentoService.processarPagamento(
                pedidoId, dto, principal.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tentativas")
    @Operation(summary = "Listar tentativas de pagamento", description = "Permissões: usuário com acesso ao pedido. Retorna o histórico em ordem cronológica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico de tentativas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem acesso ao pedido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<List<PagamentoResponseDTO>> listarTentativas(
            @PathVariable Long pedidoId,
            Principal principal) {
        Objects.requireNonNull(pedidoId, "PedidoId nao pode ser nulo");
        Objects.requireNonNull(principal, "Principal nao pode ser nulo");
        Objects.requireNonNull(principal.getName(), "Nome do usuario nao pode ser nulo");

        return ResponseEntity.ok(pagamentoService.listarTentativas(pedidoId, principal.getName()));
    }
}

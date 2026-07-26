package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.PedidoDTO;
import com.raizesdonordeste.backendapi.dto.PedidoResponseDTO;
import com.raizesdonordeste.backendapi.dto.StatusUpdateDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.model.CanalPedido;
import com.raizesdonordeste.backendapi.model.StatusPedido;
import com.raizesdonordeste.backendapi.service.PedidoService;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Criação e ciclo de vida dos pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @Operation(summary = "Criar pedido", description = "Permissões: CLIENTE ou ATENDENTE. Aceita Idempotency-Key opcional; resgate de pontos exige consentimento LGPD vigente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado ou repetição idempotente",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Header ou corpo malformado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário, unidade, produto ou estoque não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Regra de negócio, unidade inativa, estoque, consentimento ou idempotência", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Validação dos campos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PedidoResponseDTO> criarPedido(
            @Valid @RequestBody PedidoDTO dto,
            @Parameter(description = "Chave idempotente de 8 a 100 caracteres", example = "pedido-app-2026-0001")
            @RequestHeader(value = "Idempotency-Key", required = false) String chaveIdempotencia,
            Principal principal) {
        validarPrincipal(principal);
        
        log.info("POST /pedidos | Ator: {}", LogPseudonymizer.id(principal.getName()));
        PedidoResponseDTO response = pedidoService.criarPedido(dto, principal.getName(), chaveIdempotencia);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Avançar status do pedido", description = "Permissões: COZINHA, ATENDENTE, GERENTE ou ADMIN. Respeita a máquina de estados. O status CANCELADO é rejeitado; cancelamentos utilizam POST /pedidos/{id}/cancelamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado", content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem acesso ao pedido ou à unidade", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Transição inválida ou tentativa de cancelamento pela rota de status", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Status ausente ou inválido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDTO dto,
            Principal principal) {
        Objects.requireNonNull(id, "ID do pedido nao pode ser nulo");
        validarPrincipal(principal);
        
        log.info("PATCH /pedidos/{}/status | Status: {} | Ator: {}",
                id, dto.getStatus(), LogPseudonymizer.id(principal.getName()));
        PedidoResponseDTO response = pedidoService.atualizarStatus(id, dto.getStatus(), principal.getName());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancelar pedido", description = "Permissões: proprietário, ATENDENTE da unidade ou ADMIN. O pedido é bloqueado durante a transação para serializar cancelamentos concorrentes. Em pedidos pagos, solicita e aguarda a confirmação do estorno pelo gateway simulado antes de concluir o cancelamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado", content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Pedido não cancelável", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "503", description = "Gateway indisponível para processar o estorno; o pedido permanece inalterado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PedidoResponseDTO> cancelarPedido(@PathVariable Long id, Principal principal) {
        Objects.requireNonNull(id, "ID do pedido nao pode ser nulo");
        validarPrincipal(principal);
        return ResponseEntity.ok(pedidoService.cancelarPedido(id, principal.getName()));
    }

    @GetMapping
    @Operation(summary = "Listar pedidos", description = "Permissões: autenticado. Permite combinar filtros por canal, status e unidade. O escopo é limitado ao cliente ou às unidades do funcionário; ADMIN possui visão global.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de pedidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Unidade fora do escopo", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Page<PedidoResponseDTO>> listarTodos(
            @Parameter(description = "Canal de origem do pedido", example = "APP")
            @RequestParam(required = false) CanalPedido canalPedido,
            @Parameter(description = "Status atual do pedido", example = "AGUARDANDO_PAGAMENTO")
            @RequestParam(required = false) StatusPedido status,
            @Parameter(description = "Unidade do pedido", example = "1")
            @RequestParam(required = false) Long unidadeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        validarPrincipal(principal);
        log.info("GET /pedidos | canal={}, status={}, unidade={}, page={}, limit={}",
                canalPedido, status, unidadeId, page, limit);
        Pageable pageable = PageRequest.of(page, limit);
        
        return ResponseEntity.ok(pedidoService.listarFiltrado(
                canalPedido, status, unidadeId, pageable, principal.getName()));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Consultar pedido por ID", description = "Permissões: proprietário, funcionário da unidade ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado", content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem acesso ao pedido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable Long id, Principal principal) {
        Objects.requireNonNull(id, "ID do pedido nao pode ser nulo");
        validarPrincipal(principal);
        
        log.info("GET /pedidos/{}", id);
        PedidoResponseDTO response = pedidoService.buscarPorId(id, principal.getName());
        
        return ResponseEntity.ok(response);
    }

    private void validarPrincipal(Principal principal) {
        Objects.requireNonNull(principal, "Principal nao pode ser nulo");
        Objects.requireNonNull(principal.getName(), "Nome do usuario nao pode ser nulo");
    }
}

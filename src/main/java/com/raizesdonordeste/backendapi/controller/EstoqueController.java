package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.EstoqueDTO;
import com.raizesdonordeste.backendapi.dto.EstoqueResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.dto.MovimentacaoEstoqueRequestDTO;
import com.raizesdonordeste.backendapi.dto.MovimentacaoEstoqueResponseDTO;
import com.raizesdonordeste.backendapi.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Controle de quantidade de produtos por unidade")
public class EstoqueController {

    private final EstoqueService estoqueService;

    @PostMapping
    @Operation(summary = "Criar registro de estoque", description = "Exige perfil ADMIN ou GERENTE.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Estoque criado"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Produto ou unidade não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Estoque já cadastrado, produto inativo ou unidade inativa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<EstoqueResponseDTO> criar(@Valid @RequestBody EstoqueDTO dto) {
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        
        log.info("POST /estoque | ProdutoId: {} | UnidadeId: {} | Quantidade: {}", 
                dto.getProdutoId(), dto.getUnidadeId(), dto.getQuantidade());
        EstoqueResponseDTO response = estoqueService.criar(dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/unidade/{unidadeId}")
    @Operation(summary = "Listar estoque por unidade", description = "Exige perfil ADMIN ou GERENTE. Resultado paginado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Estoque listado"), @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Page<EstoqueResponseDTO>> listarPorUnidade(
            @PathVariable Long unidadeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");
        
        log.info("GET /estoque/unidade/{} | page={}, limit={}", unidadeId, page, limit);
        Pageable pageable = PageRequest.of(page, limit);
        
        return ResponseEntity.ok(estoqueService.listarPorUnidade(unidadeId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar estoque por ID", description = "Exige perfil ADMIN ou GERENTE.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Estoque encontrado"), @ApiResponse(responseCode = "404", description = "Estoque não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<EstoqueResponseDTO> buscarPorId(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID do estoque nao pode ser nulo");
        
        log.info("GET /estoque/{}", id);
        
        return ResponseEntity.ok(estoqueService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar quantidade em estoque", description = "Exige perfil ADMIN ou GERENTE.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Quantidade atualizada"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Estoque não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<EstoqueResponseDTO> atualizarQuantidade(
            @PathVariable Long id,
            @Valid @RequestBody EstoqueDTO dto) {
        Objects.requireNonNull(id, "ID do estoque nao pode ser nulo");
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        
        log.info("PUT /estoque/{} | Nova quantidade: {}", id, dto.getQuantidade());
        EstoqueResponseDTO response = estoqueService.atualizarQuantidade(id, dto);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir registro de estoque", description = "Exige perfil ADMIN ou GERENTE.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Estoque excluído"), @ApiResponse(responseCode = "404", description = "Estoque não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID do estoque nao pode ser nulo");
        
        log.info("DELETE /estoque/{}", id);
        estoqueService.deletar(id);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/movimentacoes")
    @Operation(summary = "Movimentar estoque", description = "Exige perfil ADMIN ou GERENTE. Registra entrada ou saída com motivo, saldos anterior/posterior e ator pseudonimizado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movimentação registrada", content = @Content(schema = @Schema(implementation = MovimentacaoEstoqueResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem acesso à unidade", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Estoque não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Saldo insuficiente", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Dados da movimentação inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> movimentar(
            @PathVariable Long id,
            @Valid @RequestBody MovimentacaoEstoqueRequestDTO dto,
            Principal principal) {
        validarPrincipal(principal);
        MovimentacaoEstoqueResponseDTO response = estoqueService.movimentar(id, dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/movimentacoes")
    @Operation(summary = "Consultar histórico de movimentações", description = "Exige perfil ADMIN ou GERENTE e acesso à unidade. Retorna o histórico auditável mais recente primeiro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico de movimentações"),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem acesso à unidade", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Estoque não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Page<MovimentacaoEstoqueResponseDTO>> listarMovimentacoes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        return ResponseEntity.ok(estoqueService.listarMovimentacoes(id, pageable));
    }

    private void validarPrincipal(Principal principal) {
        Objects.requireNonNull(principal, "Principal nao pode ser nulo");
        Objects.requireNonNull(principal.getName(), "Nome do usuario nao pode ser nulo");
    }
}

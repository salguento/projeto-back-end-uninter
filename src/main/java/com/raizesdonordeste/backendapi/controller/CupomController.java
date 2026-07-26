package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.CupomRequestDTO;
import com.raizesdonordeste.backendapi.dto.CupomResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.CupomService;
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

@Slf4j
@RestController
@RequestMapping("/cupons")
@RequiredArgsConstructor
@Tag(name = "Cupons", description = "Cupons de desconto utilizados nos pedidos")
public class CupomController {

    private final CupomService cupomService;

    @PostMapping
    @Operation(summary = "Criar cupom", description = "ADMIN cria cupons globais ou por unidade; GERENTE cria somente cupons das unidades vinculadas.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Cupom criado"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Código já cadastrado ou unidade inativa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<CupomResponseDTO> criar(@Valid @RequestBody CupomRequestDTO dto) {
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        
        log.info("POST /cupons | Codigo: {}", dto.getCodigo());
        CupomResponseDTO response = cupomService.criar(dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar cupons", description = "ADMIN possui visão global; GERENTE recebe somente cupons das unidades vinculadas. Resultado paginado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Cupons listados"), @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Page<CupomResponseDTO>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /cupons | page={}, limit={}", page, limit);
        Pageable pageable = PageRequest.of(page, limit);
        
        return ResponseEntity.ok(cupomService.listarTodos(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cupom por ID", description = "ADMIN possui acesso global; GERENTE consulta somente cupons das unidades vinculadas.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Cupom encontrado"), @ApiResponse(responseCode = "404", description = "Cupom não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<CupomResponseDTO> buscarPorId(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID do cupom nao pode ser nulo");
        
        log.info("GET /cupons/{}", id);
        
        return ResponseEntity.ok(cupomService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cupom", description = "ADMIN possui acesso global; GERENTE deve ter acesso às unidades de origem e destino.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Cupom atualizado"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Cupom não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Código já cadastrado, cupom em uso ou unidade de destino inativa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<CupomResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CupomRequestDTO dto) {
        Objects.requireNonNull(id, "ID do cupom nao pode ser nulo");
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        
        log.info("PUT /cupons/{} | Codigo: {}", id, dto.getCodigo());
        CupomResponseDTO response = cupomService.atualizar(id, dto);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar cupom", description = "Realiza exclusão lógica, inclusive quando existem usos históricos. ADMIN possui acesso global; GERENTE desativa somente cupons das unidades vinculadas.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Cupom desativado ou já inativo"), @ApiResponse(responseCode = "404", description = "Cupom não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID do cupom nao pode ser nulo");
        
        log.info("DELETE /cupons/{} | Desativacao logica", id);
        cupomService.desativar(id);
        
        return ResponseEntity.noContent().build();
    }
}

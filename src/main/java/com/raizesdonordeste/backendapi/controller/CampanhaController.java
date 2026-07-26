package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.CampanhaRequestDTO;
import com.raizesdonordeste.backendapi.dto.CampanhaResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.CampanhaService;
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
@RequestMapping("/campanhas")
@RequiredArgsConstructor
@Tag(name = "Campanhas", description = "Campanhas promocionais aplicadas automaticamente aos pedidos")
public class CampanhaController {

    private final CampanhaService campanhaService;

    @PostMapping
    @Operation(summary = "Criar campanha", description = "ADMIN possui acesso global; GERENTE opera somente nas unidades vinculadas. A unidade é bloqueada durante a validação para impedir campanhas sobrepostas em requisições concorrentes.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Campanha criada"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Conflito de campanha, produto inativo ou unidade inativa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<CampanhaResponseDTO> criar(@Valid @RequestBody CampanhaRequestDTO dto) {
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        
        log.info("POST /campanhas | Nome: {}", dto.getNome());
        CampanhaResponseDTO response = campanhaService.criar(dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar campanhas", description = "ADMIN possui visão global; GERENTE recebe somente campanhas das unidades vinculadas. Resultado paginado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Campanhas listadas"), @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Page<CampanhaResponseDTO>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /campanhas | page={}, limit={}", page, limit);
        Pageable pageable = PageRequest.of(page, limit);
        
        return ResponseEntity.ok(campanhaService.listarTodas(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar campanha por ID", description = "ADMIN possui acesso global; GERENTE consulta somente campanhas das unidades vinculadas.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Campanha encontrada"), @ApiResponse(responseCode = "404", description = "Campanha não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<CampanhaResponseDTO> buscarPorId(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID da campanha nao pode ser nulo");
        
        log.info("GET /campanhas/{}", id);
        
        return ResponseEntity.ok(campanhaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar campanha", description = "ADMIN possui acesso global; GERENTE deve ter acesso às unidades de origem e destino. A unidade de destino é bloqueada durante a validação de sobreposição.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Campanha atualizada"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Campanha não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Conflito de campanha, produto inativo ou unidade de destino inativa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<CampanhaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CampanhaRequestDTO dto) {
        Objects.requireNonNull(id, "ID da campanha nao pode ser nulo");
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        
        log.info("PUT /campanhas/{} | Nome: {}", id, dto.getNome());
        CampanhaResponseDTO response = campanhaService.atualizar(id, dto);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar campanha", description = "Realiza exclusão lógica. ADMIN possui acesso global; GERENTE desativa somente campanhas das unidades vinculadas.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Campanha desativada ou já inativa"), @ApiResponse(responseCode = "404", description = "Campanha não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID da campanha nao pode ser nulo");
        
        log.info("DELETE /campanhas/{} | Desativacao logica", id);
        campanhaService.desativar(id);
        
        return ResponseEntity.noContent().build();
    }
}

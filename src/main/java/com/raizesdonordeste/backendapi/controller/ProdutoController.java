package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.ProdutoEstoqueDTO;
import com.raizesdonordeste.backendapi.dto.ProdutoDTO;
import com.raizesdonordeste.backendapi.dto.ProdutoResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Catálogo de produtos e disponibilidade nas unidades")
public class ProdutoController {

    private final ProdutoService produtoService;

    @PostMapping
    @Operation(summary = "Criar produto", description = "Exige perfil ADMIN ou GERENTE.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Produto criado"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Produto já cadastrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoDTO dto) {
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        
        log.info("POST /produtos | Nome: {}", dto.getNome());
        ProdutoResponseDTO response = produtoService.criar(dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Listar produtos ativos", description = "Rota pública. Retorna somente produtos ativos, com paginação.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produtos ativos listados"), @ApiResponse(responseCode = "400", description = "Paginação inválida", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Page<ProdutoResponseDTO>> listarAtivos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /produtos | page={}, limit={}", page, limit);
        Pageable pageable = PageRequest.of(page, limit);
        
        return ResponseEntity.ok(produtoService.listarAtivos(pageable));
    }

    @GetMapping("/unidade/{unidadeId}")
    @Operation(summary = "Listar produtos com estoque em uma unidade", description = "Exige autenticação, acesso autorizado e uma unidade existente e ativa. Resultado paginado por nome do produto.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produtos disponíveis listados"), @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem acesso à unidade", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Unidade inativa", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Page<ProdutoEstoqueDTO>> listarPorUnidade(
            @PathVariable Long unidadeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Objects.requireNonNull(unidadeId, "UnidadeId nao pode ser nulo");
        
        log.info("GET /produtos/unidade/{} | page={}, limit={}", unidadeId, page, limit);
        Pageable pageable = PageRequest.of(page, limit, Sort.by("nome").ascending());
        
        return ResponseEntity.ok(produtoService.listarComEstoqueNaUnidade(unidadeId, pageable));
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Buscar produto por ID", description = "Rota pública.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produto ativo encontrado"), @ApiResponse(responseCode = "404", description = "Produto não encontrado ou inativo", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID do produto nao pode ser nulo");
        
        log.info("GET /produtos/{}", id);
        
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto", description = "Exige perfil ADMIN ou GERENTE.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produto atualizado"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoDTO dto) {
        Objects.requireNonNull(id, "ID do produto nao pode ser nulo");
        Objects.requireNonNull(dto, "DTO nao pode ser nulo");
        
        log.info("PUT /produtos/{} | Nome: {}", id, dto.getNome());
        ProdutoResponseDTO response = produtoService.atualizar(id, dto);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar produto", description = "Exige perfil ADMIN ou GERENTE. Realiza exclusão lógica.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Produto desativado"), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID do produto nao pode ser nulo");
        
        log.info("DELETE /produtos/{} | Desativacao (soft-delete)", id);
        produtoService.desativar(id);
        
        return ResponseEntity.noContent().build();
    }
}

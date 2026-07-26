package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.UnidadeDTO;
import com.raizesdonordeste.backendapi.dto.UnidadeResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.UnidadeService;
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
@RequestMapping("/unidades")
@RequiredArgsConstructor
@Tag(name = "Unidades", description = "Unidades físicas da rede")
public class UnidadeController {

	private final UnidadeService unidadeService;

	@PostMapping
	@Operation(summary = "Criar unidade", description = "Exige perfil ADMIN.")
	@ApiResponses({@ApiResponse(responseCode = "201", description = "Unidade criada"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Unidade já cadastrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
	public ResponseEntity<UnidadeResponseDTO> criar(@Valid @RequestBody UnidadeDTO dto) {
		Objects.requireNonNull(dto, "DTO nao pode ser nulo");
		log.info("POST /unidades | Nome: {}", dto.getNome());
		UnidadeResponseDTO response = unidadeService.criar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	@Operation(summary = "Listar unidades", description = "Exige autenticação. Retorna somente unidades ativas, com paginação.")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Unidades listadas"), @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
	public ResponseEntity<Page<UnidadeResponseDTO>> listarTodas(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int limit) {
		log.info("GET /unidades | page={}, limit={}", page, limit);
		Pageable pageable = PageRequest.of(page, limit);
		return ResponseEntity.ok(unidadeService.listarTodas(pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar unidade por ID", description = "Exige autenticação.")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Unidade encontrada"), @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
	public ResponseEntity<UnidadeResponseDTO> buscarPorId(@PathVariable Long id) {
		Objects.requireNonNull(id, "ID da unidade nao pode ser nulo");
		log.info("GET /unidades/{}", id);
		return ResponseEntity.ok(unidadeService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualizar unidade", description = "Exige perfil ADMIN.")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Unidade atualizada"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
	public ResponseEntity<UnidadeResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody UnidadeDTO dto) {
		Objects.requireNonNull(id, "ID da unidade nao pode ser nulo");
		Objects.requireNonNull(dto, "DTO nao pode ser nulo");
		log.info("PUT /unidades/{} | Nome: {}", id, dto.getNome());
		return ResponseEntity.ok(unidadeService.atualizar(id, dto));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Desativar unidade", description = "Exige perfil ADMIN. Realiza exclusão lógica.")
	@ApiResponses({@ApiResponse(responseCode = "204", description = "Unidade desativada"), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
	public ResponseEntity<Void> desativar(@PathVariable Long id) {
		Objects.requireNonNull(id, "ID da unidade nao pode ser nulo");
		log.info("DELETE /unidades/{} | Desativacao (soft-delete)", id);
		unidadeService.desativar(id);
		return ResponseEntity.noContent().build();
	}
}

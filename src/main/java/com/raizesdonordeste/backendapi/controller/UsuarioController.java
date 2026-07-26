package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.CadastroUsuarioDTO;
import com.raizesdonordeste.backendapi.dto.UsuarioDTO;
import com.raizesdonordeste.backendapi.dto.UsuarioResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.UsuarioService;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Cadastro, consulta e direitos do titular de dados")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Cadastrar usuário", description = "Rota pública. Cadastra exclusivamente CLIENTE e exige aceite explícito da versão vigente dos termos de uso.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Usuário cadastrado"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "E-mail já cadastrado ou documento legal desatualizado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "422", description = "Campos obrigatórios ou aceite explícito ausentes", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody CadastroUsuarioDTO dto) {
        log.info("POST /usuarios | Registro publico");
        UsuarioResponseDTO response = usuarioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Exige perfil ADMIN. Resultado paginado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuários listados"), @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Page<UsuarioResponseDTO>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /usuarios | page={}, limit={}", page, limit);
        Pageable pageable = PageRequest.of(page, limit);
        return ResponseEntity.ok(usuarioService.listarTodos(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Exige perfil ADMIN.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuário encontrado"), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        Objects.requireNonNull(id, "ID do usuario nao pode ser nulo");
        log.info("GET /usuarios/{}", id);
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Exige o próprio titular ou perfil ADMIN. Alterações de perfil são restritas.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuário atualizado"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Dados únicos já cadastrados", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioDTO dto,
            Principal principal) {
        Objects.requireNonNull(id, "ID do usuario nao pode ser nulo");
        validarPrincipal(principal);
        
        log.info("PUT /usuarios/{} | Ator: {}", id, LogPseudonymizer.id(principal.getName()));
        UsuarioResponseDTO response = usuarioService.atualizar(id, dto, principal.getName());
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir conta do usuário", description = "Exige o próprio titular ou perfil ADMIN. Anonimiza dados pessoais conforme a política LGPD, preservando registros necessários.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Conta anonimizada"), @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))), @ApiResponse(responseCode = "409", description = "Operação incompatível com o estado atual", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))})
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            Principal principal) {
        Objects.requireNonNull(id, "ID do usuario nao pode ser nulo");
        validarPrincipal(principal);
        
        log.info("DELETE /usuarios/{} | Ator: {} (LGPD)", id, LogPseudonymizer.id(principal.getName()));
        usuarioService.deletar(id, principal.getName());
        
        return ResponseEntity.noContent().build();
    }

    private void validarPrincipal(Principal principal) {
        Objects.requireNonNull(principal, "Principal nao pode ser nulo");
        Objects.requireNonNull(principal.getName(), "Nome do usuario nao pode ser nulo");
    }
}

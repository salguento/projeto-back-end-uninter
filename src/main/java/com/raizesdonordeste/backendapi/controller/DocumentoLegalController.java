package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.AceiteDocumentoResponseDTO;
import com.raizesdonordeste.backendapi.dto.AceiteTermosRequestDTO;
import com.raizesdonordeste.backendapi.dto.DocumentoLegalResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.DocumentoLegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Objects;

@RestController
@RequestMapping("/documentos-legais")
@RequiredArgsConstructor
@Tag(name = "Documentos legais", description = "Termos de uso, aviso de privacidade e aceite contratual")
public class DocumentoLegalController {

    private final DocumentoLegalService documentoLegalService;

    @GetMapping("/termos-uso")
    @SecurityRequirements
    @Operation(summary = "Consultar termos de uso vigentes", description = "Rota pública. Retorna versão, hash SHA-256 e conteúdo.")
    public ResponseEntity<DocumentoLegalResponseDTO> obterTermosUso() {
        return ResponseEntity.ok(documentoLegalService.obterTermosUso());
    }

    @GetMapping("/aviso-privacidade")
    @SecurityRequirements
    @Operation(summary = "Consultar aviso de privacidade vigente", description = "Rota pública. Não representa consentimento genérico para tratamento de dados.")
    public ResponseEntity<DocumentoLegalResponseDTO> obterAvisoPrivacidade() {
        return ResponseEntity.ok(documentoLegalService.obterAvisoPrivacidade());
    }

    @GetMapping("/termos-uso/aceite")
    @Operation(summary = "Consultar aceite dos termos", description = "Retorna se o usuário autenticado aceitou a versão e o hash vigentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Situação consultada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AceiteDocumentoResponseDTO> consultarAceite(Principal principal) {
        validarPrincipal(principal);
        return ResponseEntity.ok(documentoLegalService.consultarAceiteAtual(principal.getName()));
    }

    @PutMapping("/termos-uso/aceite")
    @Operation(summary = "Aceitar termos de uso vigentes", description = "Operação idempotente vinculada ao usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aceite registrado ou anteriormente existente"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Versão ou hash desatualizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Aceite não explícito",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AceiteDocumentoResponseDTO> aceitarTermos(
            @Valid @RequestBody AceiteTermosRequestDTO dto, Principal principal) {
        validarPrincipal(principal);
        return ResponseEntity.ok(documentoLegalService.registrarAceiteAtual(principal.getName(), dto));
    }

    private void validarPrincipal(Principal principal) {
        Objects.requireNonNull(principal, "Principal nao pode ser nulo");
        Objects.requireNonNull(principal.getName(), "Nome do usuario nao pode ser nulo");
    }
}

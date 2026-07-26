package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.ConsentimentoRequestDTO;
import com.raizesdonordeste.backendapi.dto.ConsentimentoResponseDTO;
import com.raizesdonordeste.backendapi.dto.SaldoPontosResponseDTO;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.service.FidelidadeService;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/fidelidade")
@RequiredArgsConstructor
@Tag(name = "Fidelidade", description = "Saldo de pontos e consentimento LGPD")
public class FidelidadeController {

    private final FidelidadeService fidelidadeService;

    @GetMapping("/saldo")
    @Operation(summary = "Consultar saldo de pontos", description = "Permissões: usuário autenticado; consulta somente o próprio saldo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo consultado", content = @Content(schema = @Schema(implementation = SaldoPontosResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<SaldoPontosResponseDTO> consultarSaldo(Principal principal) {
        Objects.requireNonNull(principal, "Principal nao pode ser nulo");
        Objects.requireNonNull(principal.getName(), "Nome do usuario nao pode ser nulo");
        
        log.info("GET /fidelidade/saldo | Ator: {}", LogPseudonymizer.id(principal.getName()));
        SaldoPontosResponseDTO response = fidelidadeService.consultarSaldo(principal.getName());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consentimento")
    @Operation(summary = "Consultar consentimento de fidelidade", description = "Retorna finalidade, versão vigente e situação do consentimento do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Situação do consentimento", content = @Content(schema = @Schema(implementation = ConsentimentoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ConsentimentoResponseDTO> consultarConsentimento(Principal principal) {
        validarPrincipal(principal);
        return ResponseEntity.ok(fidelidadeService.consultarConsentimento(principal.getName()));
    }

    @PostMapping("/consentimento")
    @Operation(summary = "Conceder consentimento de fidelidade", description = "Registra evento auditável de aceite explícito para a versão vigente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Consentimento registrado", content = @Content(schema = @Schema(implementation = ConsentimentoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Aceite não explícito", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ConsentimentoResponseDTO> concederConsentimento(
            @Valid @RequestBody ConsentimentoRequestDTO request, Principal principal) {
        validarPrincipal(principal);
        ConsentimentoResponseDTO response = fidelidadeService.concederConsentimento(principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/consentimento")
    @Operation(summary = "Revogar consentimento de fidelidade", description = "Registra evento auditável de revogação sem apagar o histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consentimento revogado", content = @Content(schema = @Schema(implementation = ConsentimentoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ConsentimentoResponseDTO> revogarConsentimento(Principal principal) {
        validarPrincipal(principal);
        return ResponseEntity.ok(fidelidadeService.revogarConsentimento(principal.getName()));
    }

    private void validarPrincipal(Principal principal) {
        Objects.requireNonNull(principal, "Principal nao pode ser nulo");
        Objects.requireNonNull(principal.getName(), "Nome do usuario nao pode ser nulo");
    }
}

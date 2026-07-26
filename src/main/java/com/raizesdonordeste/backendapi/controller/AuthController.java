package com.raizesdonordeste.backendapi.controller;

import com.raizesdonordeste.backendapi.dto.LoginRequestDTO;
import com.raizesdonordeste.backendapi.dto.LoginResponseDTO;
import com.raizesdonordeste.backendapi.security.JwtTokenService;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Emissão de tokens JWT")
public class AuthController {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autenticar usuário", description = "Rota pública. Valida e-mail e senha e retorna um token JWT Bearer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Campos obrigatórios inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        Objects.requireNonNull(loginRequest, "LoginRequest nao pode ser nulo");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getSenha()
                )
        );

        String token = jwtTokenService.gerarToken(authentication);

        log.info("Login realizado | Ator: {}", LogPseudonymizer.id(loginRequest.getEmail()));

        LoginResponseDTO response = new LoginResponseDTO(token, TOKEN_TYPE);

        return ResponseEntity.ok(response);
    }
}

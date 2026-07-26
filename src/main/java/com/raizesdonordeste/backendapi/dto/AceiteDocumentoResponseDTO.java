package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.TipoDocumentoLegal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        description = "Situação do aceite contratual do usuário autenticado",
        example = "{\"tipo\":\"TERMOS_USO\",\"versao\":\"1.0\",\"hashSha256\":\"a6f8b5c3d77b0c6bfc4d6a62d6b6940e8a70289389436d29fad4f15b991062af\",\"aceitoEm\":\"2026-07-20T14:30:00\",\"vigente\":true}")
public record AceiteDocumentoResponseDTO(
        TipoDocumentoLegal tipo,
        String versao,
        String hashSha256,
        LocalDateTime aceitoEm,
        boolean vigente) {
}

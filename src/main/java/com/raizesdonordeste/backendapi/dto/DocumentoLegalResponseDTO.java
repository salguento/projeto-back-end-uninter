package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.TipoDocumentoLegal;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Documento legal vigente publicado pela aplicação",
        example = "{\"tipo\":\"TERMOS_USO\",\"versao\":\"1.0\",\"hashSha256\":\"a6f8b5c3d77b0c6bfc4d6a62d6b6940e8a70289389436d29fad4f15b991062af\",\"conteudo\":\"Termos de Uso — versão 1.0\"}")
public record DocumentoLegalResponseDTO(
        TipoDocumentoLegal tipo,
        String versao,
        String hashSha256,
        String conteudo) {
}

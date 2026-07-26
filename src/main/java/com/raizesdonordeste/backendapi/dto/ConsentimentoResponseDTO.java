package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.FinalidadeConsentimento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "Situação do consentimento para uma finalidade específica",
        example = "{\"finalidade\":\"FIDELIDADE\",\"descricaoFinalidade\":\"Uso de pontos no programa de fidelidade\",\"ativo\":true,\"versaoTermoAtual\":\"1.0\",\"versaoRegistrada\":\"1.0\",\"registradoEm\":\"2026-07-20T14:30:00\"}")
public class ConsentimentoResponseDTO {
    private FinalidadeConsentimento finalidade;
    private String descricaoFinalidade;
    private Boolean ativo;
    private String versaoTermoAtual;
    private String versaoRegistrada;
    private LocalDateTime registradoEm;
}

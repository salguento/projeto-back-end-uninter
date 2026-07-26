package com.raizesdonordeste.backendapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
        description = "Unidade operacional cadastrada",
        example = "{\"id\":1,\"nome\":\"Unidade Centro\",\"endereco\":\"Rua das Flores, 100\",\"cidade\":\"Recife\",\"estado\":\"PE\",\"cep\":\"50000-000\",\"telefone\":\"8133334444\"}")
public class UnidadeResponseDTO {

    private Long id;
    private String nome;
    private String endereco;
    private String cidade;
    private String estado;
    private String cep;
    private String telefone;
}

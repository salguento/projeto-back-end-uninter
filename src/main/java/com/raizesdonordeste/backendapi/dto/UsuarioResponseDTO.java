package com.raizesdonordeste.backendapi.dto;

import com.raizesdonordeste.backendapi.model.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
        description = "Usuário retornado sem exposição da senha",
        example = "{\"id\":1,\"nome\":\"Cliente Exemplo\",\"email\":\"cliente@exemplo.com\",\"perfil\":\"CLIENTE\",\"pontos\":120,\"telefone\":\"11987654321\",\"cpf\":\"11122233344\"}")
public class UsuarioResponseDTO {
    private Long id;
    private String nome;
    private String email;
    private Perfil perfil;
    private Integer pontos;
    private String telefone;
    private String cpf;
}

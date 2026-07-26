package com.raizesdonordeste.backendapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");
    private static final Set<String> RESPONSE_SCHEMAS_WITH_EXAMPLES = Set.of(
            "AceiteDocumentoResponseDTO",
            "CampanhaResponseDTO",
            "ConsentimentoResponseDTO",
            "CupomAplicadoDTO",
            "CupomResponseDTO",
            "DocumentoLegalResponseDTO",
            "ErrorResponseDTO",
            "EstoqueResponseDTO",
            "ItemPedidoResponseDTO",
            "LoginResponseDTO",
            "MovimentacaoEstoqueResponseDTO",
            "PagamentoResponseDTO",
            "PedidoResponseDTO",
            "ProdutoEstoqueDTO",
            "ProdutoResponseDTO",
            "SaldoPontosResponseDTO",
            "UnidadeResponseDTO",
            "UsuarioResponseDTO");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void devePublicarContratoCompletoComSegurancaRespostasEExemplos() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode document = objectMapper.readTree(body);

        assertThat(document.at("/info/title").asText()).contains("Raízes do Nordeste");
        assertThat(document.at("/servers/0/url").asText()).isEqualTo("/api");
        assertThat(document.at("/components/securitySchemes/bearerAuth/type").asText()).isEqualTo("http");
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(document.at("/security/0/bearerAuth").isArray()).isTrue();

        JsonNode paths = document.path("paths");
        int operationCount = 0;
        var pathIterator = paths.fields();
        while (pathIterator.hasNext()) {
            var path = pathIterator.next();
            var operationIterator = path.getValue().fields();
            while (operationIterator.hasNext()) {
                var operation = operationIterator.next();
                if (!HTTP_METHODS.contains(operation.getKey())) {
                    continue;
                }
                operationCount++;
                JsonNode definition = operation.getValue();
                assertThat(definition.path("summary").asText()).isNotBlank();
                assertThat(definition.path("responses").size()).isPositive();
                boolean hasSuccess = false;
                var responseIterator = definition.path("responses").fields();
                while (responseIterator.hasNext()) {
                    var response = responseIterator.next();
                    if (response.getKey().startsWith("2")) {
                        hasSuccess = true;
                        if (!response.getKey().equals("204")) {
                            assertThat(response.getValue().path("content").size()).isPositive();
                        }
                    }
                }
                assertThat(hasSuccess).isTrue();
                if (definition.path("security").isMissingNode()) {
                    assertThat(definition.path("responses").has("401")).isTrue();
                    assertThat(definition.path("responses").has("403")).isTrue();
                    assertThat(definition.at("/responses/401/content/application~1json/schema/$ref").asText())
                            .endsWith("/ErrorResponseDTO");
                }
            }
        }

        assertThat(operationCount).isEqualTo(49);
        assertThat(document.at("/paths/~1auth~1login/post/security").isArray()).isTrue();
        assertThat(document.at("/paths/~1auth~1login/post/security").isEmpty()).isTrue();
        assertThat(document.at("/paths/~1usuarios/post/security").isEmpty()).isTrue();
        assertThat(document.at("/paths/~1produtos/get/security").isEmpty()).isTrue();
        assertThat(document.at("/paths/~1documentos-legais~1termos-uso/get/security").isEmpty()).isTrue();
        assertThat(document.at("/paths/~1documentos-legais~1aviso-privacidade/get/security").isEmpty()).isTrue();

        assertThat(document.at("/paths/~1pedidos/post/parameters"))
                .anyMatch(parameter -> "Idempotency-Key".equals(parameter.path("name").asText()));
        assertThat(document.at("/paths/~1pedidos/get/parameters"))
                .anyMatch(parameter -> "status".equals(parameter.path("name").asText()));
        Set.of("/unidades", "/produtos/unidade/{unidadeId}", "/campanhas", "/cupons")
                .forEach(path -> {
                    JsonNode parameters = document.path("paths").path(path).path("get").path("parameters");
                    assertThat(parameters)
                            .as("Listagem %s deve declarar o parâmetro page", path)
                            .anyMatch(parameter -> "page".equals(parameter.path("name").asText()));
                    assertThat(parameters)
                            .as("Listagem %s deve declarar o parâmetro limit", path)
                            .anyMatch(parameter -> "limit".equals(parameter.path("name").asText()));
                });
        JsonNode statusPedidoPath = document.at("/paths/~1pedidos~1{id}~1status");
        assertThat(statusPedidoPath.has("patch")).isTrue();
        assertThat(statusPedidoPath.has("put")).isFalse();
        assertThat(document.at("/paths/~1estoque~1{id}~1movimentacoes/post/responses/201").isObject())
                .isTrue();
        assertThat(document.at("/paths/~1pedidos~1{pedidoId}~1pagamento/post/responses/503/content/application~1json/schema/$ref").asText())
                .endsWith("/ErrorResponseDTO");
        assertThat(document.at("/components/schemas/PedidoDTO/properties/unidadeId/example").asLong())
                .isEqualTo(1L);
        assertThat(document.at("/components/schemas/ErrorResponseDTO/properties/requestId/example").asText())
                .isNotBlank();
        RESPONSE_SCHEMAS_WITH_EXAMPLES.forEach(schemaName -> {
            JsonNode schema = document.at("/components/schemas/" + schemaName);
            assertThat(schema.isMissingNode())
                    .as("Esquema de resposta %s deve existir", schemaName)
                    .isFalse();
            assertThat(schema.path("example").isMissingNode() || schema.path("example").isNull())
                    .as("Esquema de resposta %s deve possuir exemplo JSON", schemaName)
                    .isFalse();
        });
    }
}

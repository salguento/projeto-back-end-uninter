package com.raizesdonordeste.backendapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ERROR_SCHEMA = "#/components/schemas/ErrorResponseDTO";

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Raízes do Nordeste - API Backend")
                        .description("API REST do MVP de pedidos da rede Raízes do Nordeste. "
                                + "As rotas protegidas usam JWT Bearer. Erros seguem o schema ErrorResponseDTO "
                                + "e incluem requestId para correlação. A criação de pedidos aceita "
                                + "Idempotency-Key para repetição segura da requisição.")
                        .version("v1.0"))
                .addServersItem(new Server()
                        .url("/api")
                        .description("Contexto da aplicação"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer protectedOperationsResponses() {
        return openApi -> openApi.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .forEach(operation -> {
                    if (operation.getSecurity() == null || !operation.getSecurity().isEmpty()) {
                        operation.getResponses().put("401", errorResponse("Não autenticado"));
                        operation.getResponses().put("403", errorResponse("Sem permissão para a operação"));
                    }
                    operation.getResponses().forEach((code, response) -> {
                        if (code.startsWith("4") || code.startsWith("5")) {
                            response.setContent(errorContent());
                        }
                    });
                });
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(errorContent());
    }

    private Content errorContent() {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA)));
    }
}

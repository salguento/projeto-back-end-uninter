package com.raizesdonordeste.backendapi.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracaoAmbienteTest {

    private static final Path DIRETORIO = Path.of("src", "main", "resources");

    @Test
    void configuracaoBaseDeveExigirSegredoExternoEManterSwaggerDesabilitado() throws IOException {
        Properties properties = carregar("application.properties");

        assertEquals("${JWT_SECRET}", properties.getProperty("api.security.token.secret"));
        assertEquals("false", properties.getProperty("springdoc.api-docs.enabled"));
        assertEquals("false", properties.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("never", properties.getProperty("server.error.include-message"));
    }

    @Test
    void perfilProducaoDeveExigirBancoExternoEDesabilitarRecursosDeDesenvolvimento() throws IOException {
        Properties properties = carregar("application-prod.properties");

        assertEquals("${DB_URL}", properties.getProperty("spring.datasource.url"));
        assertEquals("${DB_USERNAME}", properties.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));
        assertEquals("false", properties.getProperty("spring.h2.console.enabled"));
        assertEquals("false", properties.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("true", properties.getProperty("spring.flyway.clean-disabled"));
        assertEquals("", properties.getProperty("app.pagamento.forcar-status"));
    }

    @Test
    void perfilDesenvolvimentoDeveUsarSomenteRecursosLocais() throws IOException {
        Properties properties = carregar("application-dev.properties");

        assertTrue(properties.getProperty("spring.datasource.url").startsWith("jdbc:h2:mem:"));
        assertEquals("${H2_CONSOLE_ENABLED:true}", properties.getProperty("spring.h2.console.enabled"));
        assertEquals("${SWAGGER_ENABLED:true}", properties.getProperty("springdoc.swagger-ui.enabled"));
        assertTrue(properties.getProperty("api.security.token.secret").startsWith("${JWT_SECRET:"));
    }

    private Properties carregar(String arquivo) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(DIRETORIO.resolve(arquivo))) {
            properties.load(input);
        }
        return properties;
    }
}

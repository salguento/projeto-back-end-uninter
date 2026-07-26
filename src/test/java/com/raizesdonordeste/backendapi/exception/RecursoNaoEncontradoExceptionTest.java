package com.raizesdonordeste.backendapi.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecursoNaoEncontradoException - Testes")
class RecursoNaoEncontradoExceptionTest {

    @Test
    @DisplayName("Deve criar excecao com codigo e mensagem")
    void deveCriarExcecaoComCodigoEMensagem() {
        RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException(
                "PRODUTO_NAO_ENCONTRADO", "Produto ID 999 nao encontrado");

        assertEquals("PRODUTO_NAO_ENCONTRADO", ex.getErrorCode());
        assertEquals("Produto ID 999 nao encontrado", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lancar NullPointerException se codigo for nulo")
    void deveLancarNPESeCodigoNulo() {
        assertThrows(NullPointerException.class,
                () -> new RecursoNaoEncontradoException(null, "mensagem"));
    }

    @Test
    @DisplayName("Deve lancar NullPointerException se mensagem for nula")
    void deveLancarNPESeMensagemNula() {
        assertThrows(NullPointerException.class,
                () -> new RecursoNaoEncontradoException("CODIGO", null));
    }

    @Test
    @DisplayName("Deve lancar IllegalArgumentException se codigo for vazio")
    void deveLancarIAESeCodigoVazio() {
        assertThrows(IllegalArgumentException.class,
                () -> new RecursoNaoEncontradoException("  ", "mensagem"));
    }

    @Test
    @DisplayName("Deve criar excecao com causa")
    void deveCriarExcecaoComCausa() {
        RuntimeException causa = new RuntimeException("causa original");
        RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException(
                "CODIGO", "mensagem", causa);

        assertEquals(causa, ex.getCause());
    }
}
package com.raizesdonordeste.backendapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogPseudonymizerTest {

    @Test
    void deveGerarIdentificadorEstavelSemExporEmail() {
        String primeiro = LogPseudonymizer.id("cliente@raizes.com");
        String segundo = LogPseudonymizer.id(" CLIENTE@RAIZES.COM ");

        assertEquals(primeiro, segundo);
        assertTrue(primeiro.startsWith("usr_"));
        assertFalse(primeiro.contains("cliente"));
        assertFalse(primeiro.contains("@"));
    }

    @Test
    void deveRepresentarIdentificadorAusenteComoAnonimo() {
        assertEquals("anonimo", LogPseudonymizer.id(null));
        assertEquals("anonimo", LogPseudonymizer.id(" "));
    }
}

package com.raizesdonordeste.backendapi.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class LogPseudonymizer {

    private static final int TAMANHO_IDENTIFICADOR = 12;

    private LogPseudonymizer() {
    }

    public static String id(String identificadorPessoal) {
        if (identificadorPessoal == null || identificadorPessoal.isBlank()) {
            return "anonimo";
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    identificadorPessoal.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return "usr_" + HexFormat.of().formatHex(hash).substring(0, TAMANHO_IDENTIFICADOR);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel.", e);
        }
    }
}

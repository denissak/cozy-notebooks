package com.cozy.notebooks.service.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 helper for opaque refresh tokens (matches {@code CHAR(64)} hex hashes).
 */
@Component
public class Sha256Hex {

    private static final HexFormat HEX = HexFormat.of();

    public String hashUtf8(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] sum = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(sum);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

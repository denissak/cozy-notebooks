package com.cozy.notebooks.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes a deterministic SHA-256 hash of a page/template content JSON tree.
 *
 * <p>The hash is taken over {@code ObjectMapper.writeValueAsBytes(node)} and
 * returned as a 64-char lowercase hex string. The wire-format encoding of a
 * {@link JsonNode} is stable for a given tree: object fields preserve their
 * insertion order, and primitives serialize identically. The hash is therefore
 * deterministic for the same {@code JsonNode} structure.
 *
 * <p>Conflict detection compares stored {@code content_hash} strings, never
 * recomputed hashes from MySQL-read trees, so any potential JSON key
 * reordering performed by MySQL's binary JSON storage is irrelevant here.
 */
@Service
public class PageContentHashService {

    private final ObjectMapper objectMapper;

    public PageContentHashService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String hash(JsonNode content) {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        try {
            byte[] payload = objectMapper.writeValueAsBytes(content);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] sum = digest.digest(payload);
            return toHexLower(sum);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize content for hashing", e);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory in every Java SE platform — should never happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHexLower(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            hex[i * 2]     = HEX[v >>> 4];
            hex[i * 2 + 1] = HEX[v & 0x0f];
        }
        return new String(hex);
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();
}

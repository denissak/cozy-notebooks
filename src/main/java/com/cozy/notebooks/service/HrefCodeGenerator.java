package com.cozy.notebooks.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * URL-safe lowercase href codes ({@code href_code}), 18 characters long.
 */
@Component
public class HrefCodeGenerator {

    public static final String ALPHABET = "abcdefghijkmnopqrstuvwxyz23456789";
    private static final int LENGTH = 18;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}

package com.gestao.api.dto.auth;

public record TokenResponse(
        String token,
        String tipo,
        Long expiraEm
) {
    public TokenResponse(String token, Long expiraEm) {
        this(token, "Bearer", expiraEm);
    }
}
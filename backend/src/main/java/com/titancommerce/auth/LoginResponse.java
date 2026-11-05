package com.titancommerce.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
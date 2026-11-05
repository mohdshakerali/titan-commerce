package com.titancommerce.customer.dto;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        Instant createdAt
) {
}
package com.biblioteca.auth_service.dto;

public record LoginResponse(
        String token,
        String tipo,
        String email,
        String rol
) {}
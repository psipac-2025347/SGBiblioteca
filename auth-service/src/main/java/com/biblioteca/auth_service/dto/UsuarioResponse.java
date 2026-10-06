package com.biblioteca.auth_service.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String estado,
        String rol
) {}
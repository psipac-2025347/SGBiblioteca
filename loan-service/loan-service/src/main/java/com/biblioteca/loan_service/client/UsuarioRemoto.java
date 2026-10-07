package com.biblioteca.loan_service.client;

public record UsuarioRemoto(
        Long id,
        String nombre,
        String email,
        String estado,
        String rol
) {}
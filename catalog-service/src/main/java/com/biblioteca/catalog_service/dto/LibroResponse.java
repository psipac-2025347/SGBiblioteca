package com.biblioteca.catalog_service.dto;

public record LibroResponse(
        Long id,
        String isbn,
        String titulo,
        String autor,
        String categoria,
        Integer stockTotal,
        Integer stockDisponible
) {}
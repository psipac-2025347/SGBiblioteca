package com.biblioteca.catalog_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LibroRequest(
        @NotBlank String isbn,
        @NotBlank String titulo,
        @NotBlank String autor,
        @NotBlank String categoria,
        @NotNull @Min(0) Integer stockTotal
) {}
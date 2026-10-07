package com.biblioteca.loan_service.dto;

import jakarta.validation.constraints.NotNull;

public record PrestamoRequest(
        @NotNull Long usuarioId,
        @NotNull Long libroId
) {}
package com.biblioteca.loan_service.dto;

import java.time.LocalDate;

public record PrestamoResponse(
        Long id,
        Long usuarioId,
        Long libroId,
        LocalDate fechaPrestamo,
        LocalDate fechaDevolucionEsperada,
        LocalDate fechaDevolucionReal,
        String estado
) {}
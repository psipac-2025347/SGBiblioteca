package com.biblioteca.catalog_service.dto;

import jakarta.validation.constraints.NotNull;

public record StockRequest(
        @NotNull Integer cambio   // -1 al prestar, +1 al devolver
) {}
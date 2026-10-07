package com.biblioteca.auth_service.dto;

import com.biblioteca.auth_service.entity.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull EstadoUsuario estado   // ACTIVO o SANCIONADO
) {}
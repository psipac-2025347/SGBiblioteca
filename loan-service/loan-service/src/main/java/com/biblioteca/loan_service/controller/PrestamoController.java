package com.biblioteca.loan_service.controller;

import com.biblioteca.loan_service.dto.PrestamoRequest;
import com.biblioteca.loan_service.dto.PrestamoResponse;
import com.biblioteca.loan_service.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    public ResponseEntity<PrestamoResponse> registrar(
            @Valid @RequestBody PrestamoRequest request,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prestamoService.registrar(request, authorization));
    }

    @PatchMapping("/{id}/devolucion")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    public ResponseEntity<PrestamoResponse> devolver(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(prestamoService.devolver(id, authorization));
    }

    @GetMapping("/mis-prestamos")
    @PreAuthorize("hasRole('LECTOR')")
    public ResponseEntity<List<PrestamoResponse>> misPrestamos(Authentication authentication) {
        Long usuarioId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(prestamoService.misPrestamos(usuarioId));
    }

    @GetMapping("/atrasados")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    public ResponseEntity<List<PrestamoResponse>> atrasados() {
        return ResponseEntity.ok(prestamoService.atrasados());
    }
}
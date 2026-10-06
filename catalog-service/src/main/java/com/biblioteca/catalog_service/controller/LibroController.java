package com.biblioteca.catalog_service.controller;

import com.biblioteca.catalog_service.dto.LibroRequest;
import com.biblioteca.catalog_service.dto.LibroResponse;
import com.biblioteca.catalog_service.dto.PageResponse;
import com.biblioteca.catalog_service.service.LibroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.biblioteca.catalog_service.dto.StockRequest;

@RestController
@RequestMapping("/api/v1/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    @GetMapping
    public ResponseEntity<PageResponse<LibroResponse>> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String categoria,
            Pageable pageable) {
        return ResponseEntity.ok(libroService.listar(titulo, categoria, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibroResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(libroService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LibroResponse> crear(@Valid @RequestBody LibroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LibroResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody LibroRequest request) {
        return ResponseEntity.ok(libroService.actualizar(id, request));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    public ResponseEntity<LibroResponse> ajustarStock(@PathVariable Long id,
                                                      @Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(libroService.ajustarStock(id, request.cambio()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
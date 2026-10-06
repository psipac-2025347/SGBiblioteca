package com.biblioteca.catalog_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "libros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 150)
    private String autor;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(name = "stock_total", nullable = false)
    private Integer stockTotal;

    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;
}
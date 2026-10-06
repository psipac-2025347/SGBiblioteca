package com.biblioteca.catalog_service.repository;

import com.biblioteca.catalog_service.entity.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @Query("""
            SELECT l FROM Libro l
            WHERE (:titulo IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
              AND (:categoria IS NULL OR LOWER(l.categoria) = LOWER(:categoria))
            """)
    Page<Libro> buscar(@Param("titulo") String titulo,
                       @Param("categoria") String categoria,
                       Pageable pageable);
}
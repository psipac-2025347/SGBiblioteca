package com.biblioteca.catalog_service.service;

import com.biblioteca.catalog_service.dto.LibroRequest;
import com.biblioteca.catalog_service.dto.LibroResponse;
import com.biblioteca.catalog_service.dto.PageResponse;
import com.biblioteca.catalog_service.entity.Libro;
import com.biblioteca.catalog_service.exception.BusinessRuleException;
import com.biblioteca.catalog_service.exception.ResourceNotFoundException;
import com.biblioteca.catalog_service.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;

    @Transactional(readOnly = true)
    public PageResponse<LibroResponse> listar(String titulo, String categoria, Pageable pageable) {
        String t = (titulo == null || titulo.isBlank()) ? null : titulo.trim();
        String c = (categoria == null || categoria.isBlank()) ? null : categoria.trim();

        Page<Libro> page = libroRepository.buscar(t, c, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public LibroResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public LibroResponse crear(LibroRequest request) {
        if (libroRepository.existsByIsbn(request.isbn())) {
            throw new BusinessRuleException("Ya existe un libro con ese ISBN");
        }
        Libro libro = Libro.builder()
                .isbn(request.isbn())
                .titulo(request.titulo())
                .autor(request.autor())
                .categoria(request.categoria())
                .stockTotal(request.stockTotal())
                .stockDisponible(request.stockTotal())
                .build();
        return toResponse(libroRepository.save(libro));
    }

    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest request) {
        Libro libro = buscarPorId(id);

        if (libroRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
            throw new BusinessRuleException("Ya existe otro libro con ese ISBN");
        }

        int diferencia = request.stockTotal() - libro.getStockTotal();
        int nuevoDisponible = libro.getStockDisponible() + diferencia;
        if (nuevoDisponible < 0) {
            throw new BusinessRuleException(
                    "El stock total no puede ser menor que los ejemplares actualmente prestados");
        }

        libro.setIsbn(request.isbn());
        libro.setTitulo(request.titulo());
        libro.setAutor(request.autor());
        libro.setCategoria(request.categoria());
        libro.setStockTotal(request.stockTotal());
        libro.setStockDisponible(nuevoDisponible);

        return toResponse(libroRepository.save(libro));
    }

    @Transactional
    public void eliminar(Long id) {
        libroRepository.delete(buscarPorId(id));
    }

    @Transactional
    public LibroResponse ajustarStock(Long id, int cambio) {
        if (cambio == 0) {
            throw new BusinessRuleException("El cambio de stock no puede ser 0");
        }

        Libro libro = libroRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id " + id));

        int nuevo = libro.getStockDisponible() + cambio;

        if (nuevo < 0) {
            throw new BusinessRuleException("No hay stock disponible para este libro");
        }
        if (nuevo > libro.getStockTotal()) {
            throw new BusinessRuleException("El stock disponible no puede superar el stock total");
        }

        libro.setStockDisponible(nuevo);
        return toResponse(libroRepository.save(libro));
    }

    private Libro buscarPorId(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id " + id));
    }

    private LibroResponse toResponse(Libro l) {
        return new LibroResponse(l.getId(), l.getIsbn(), l.getTitulo(), l.getAutor(),
                l.getCategoria(), l.getStockTotal(), l.getStockDisponible());
    }
}
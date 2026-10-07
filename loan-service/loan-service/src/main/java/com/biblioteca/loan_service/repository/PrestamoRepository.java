package com.biblioteca.loan_service.repository;

import com.biblioteca.loan_service.entity.EstadoPrestamo;
import com.biblioteca.loan_service.entity.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    long countByUsuarioIdAndEstadoIn(Long usuarioId, Collection<EstadoPrestamo> estados);

    boolean existsByUsuarioIdAndEstadoInAndFechaDevolucionEsperadaBefore(
            Long usuarioId, Collection<EstadoPrestamo> estados, LocalDate fecha);

    List<Prestamo> findByEstadoAndFechaDevolucionEsperadaBefore(EstadoPrestamo estado, LocalDate fecha);

    List<Prestamo> findByEstado(EstadoPrestamo estado);

    List<Prestamo> findByUsuarioIdOrderByFechaPrestamoDesc(Long usuarioId);
}
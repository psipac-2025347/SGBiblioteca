package com.biblioteca.loan_service.service;

import com.biblioteca.loan_service.client.AuthClient;
import com.biblioteca.loan_service.client.CatalogClient;
import com.biblioteca.loan_service.client.UsuarioRemoto;
import com.biblioteca.loan_service.dto.PrestamoRequest;
import com.biblioteca.loan_service.dto.PrestamoResponse;
import com.biblioteca.loan_service.entity.EstadoPrestamo;
import com.biblioteca.loan_service.entity.Prestamo;
import com.biblioteca.loan_service.exception.BusinessRuleException;
import com.biblioteca.loan_service.exception.ResourceNotFoundException;
import com.biblioteca.loan_service.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private static final int MAX_PRESTAMOS_ACTIVOS = 3;
    private static final int DIAS_PRESTAMO = 14;
    private static final List<EstadoPrestamo> NO_DEVUELTOS =
            List.of(EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO);

    private final PrestamoRepository prestamoRepository;
    private final AuthClient authClient;
    private final CatalogClient catalogClient;

    @Transactional
    public PrestamoResponse registrar(PrestamoRequest request, String authorization) {
        // 1. El usuario existe (404 si no)
        UsuarioRemoto usuario = authClient.obtenerUsuario(request.usuarioId(), authorization);

        // 2. Usuario sancionado: rechazar
        if ("SANCIONADO".equals(usuario.estado())) {
            throw new BusinessRuleException("El usuario está sancionado y no puede realizar préstamos");
        }

        LocalDate hoy = LocalDate.now();

        // 3. Préstamo vencido sin devolver: se sanciona al usuario y se rechaza
        if (prestamoRepository.existsByUsuarioIdAndEstadoInAndFechaDevolucionEsperadaBefore(
                usuario.id(), NO_DEVUELTOS, hoy)) {
            authClient.sancionar(usuario.id(), authorization);
            throw new BusinessRuleException(
                    "El usuario tiene préstamos vencidos sin devolver; fue marcado como SANCIONADO");
        }

        // 4. Máximo 3 préstamos activos para un LECTOR
        if ("LECTOR".equals(usuario.rol())
                && prestamoRepository.countByUsuarioIdAndEstadoIn(usuario.id(), NO_DEVUELTOS)
                >= MAX_PRESTAMOS_ACTIVOS) {
            throw new BusinessRuleException(
                    "El lector ya tiene " + MAX_PRESTAMOS_ACTIVOS + " préstamos activos");
        }

        // 5. Registrar préstamo (fecha de hoy + 14 días)
        Prestamo prestamo = Prestamo.builder()
                .usuarioId(usuario.id())
                .libroId(request.libroId())
                .fechaPrestamo(hoy)
                .fechaDevolucionEsperada(hoy.plusDays(DIAS_PRESTAMO))
                .estado(EstadoPrestamo.ACTIVO)
                .build();
        prestamo = prestamoRepository.save(prestamo);

        // 6. Libro existe y tiene stock: se descuenta en catalog-service.
        //    Si falla (404 / sin stock), la excepción revierte el préstamo guardado.
        catalogClient.ajustarStock(request.libroId(), -1, authorization);

        return toResponse(prestamo);
    }

    @Transactional
    public PrestamoResponse devolver(Long id, String authorization) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id " + id));

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new BusinessRuleException("El préstamo ya fue devuelto");
        }

        prestamo.setFechaDevolucionReal(LocalDate.now());
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo = prestamoRepository.save(prestamo);

        catalogClient.ajustarStock(prestamo.getLibroId(), 1, authorization);

        return toResponse(prestamo);
    }

    @Transactional
    public List<PrestamoResponse> misPrestamos(Long usuarioId) {
        marcarAtrasados();
        return prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDesc(usuarioId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<PrestamoResponse> atrasados() {
        marcarAtrasados();
        return prestamoRepository.findByEstado(EstadoPrestamo.ATRASADO)
                .stream().map(this::toResponse).toList();
    }

    /** Pasa a ATRASADO los préstamos ACTIVOS cuya fecha esperada ya pasó. */
    private void marcarAtrasados() {
        List<Prestamo> vencidos = prestamoRepository
                .findByEstadoAndFechaDevolucionEsperadaBefore(EstadoPrestamo.ACTIVO, LocalDate.now());
        vencidos.forEach(p -> p.setEstado(EstadoPrestamo.ATRASADO));
        prestamoRepository.saveAll(vencidos);
    }

    private PrestamoResponse toResponse(Prestamo p) {
        return new PrestamoResponse(p.getId(), p.getUsuarioId(), p.getLibroId(),
                p.getFechaPrestamo(), p.getFechaDevolucionEsperada(),
                p.getFechaDevolucionReal(), p.getEstado().name());
    }
}
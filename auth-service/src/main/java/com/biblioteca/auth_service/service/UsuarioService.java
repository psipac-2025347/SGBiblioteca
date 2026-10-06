package com.biblioteca.auth_service.service;

import com.biblioteca.auth_service.dto.UsuarioResponse;
import com.biblioteca.auth_service.entity.EstadoUsuario;
import com.biblioteca.auth_service.entity.Usuario;
import com.biblioteca.auth_service.exception.ResourceNotFoundException;
import com.biblioteca.auth_service.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, EstadoUsuario estado) {
        Usuario usuario = buscar(id);
        usuario.setEstado(estado);
        return toResponse(usuarioRepository.save(usuario));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(),
                u.getEstado().name(), u.getRol().getNombre());
    }
}
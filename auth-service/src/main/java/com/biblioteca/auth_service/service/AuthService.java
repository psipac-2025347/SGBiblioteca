package com.biblioteca.auth_service.service;

import com.biblioteca.auth_service.dto.LoginRequest;
import com.biblioteca.auth_service.dto.LoginResponse;
import com.biblioteca.auth_service.dto.RegisterRequest;
import com.biblioteca.auth_service.dto.UsuarioResponse;
import com.biblioteca.auth_service.entity.EstadoUsuario;
import com.biblioteca.auth_service.entity.Rol;
import com.biblioteca.auth_service.entity.Usuario;
import com.biblioteca.auth_service.exception.BusinessRuleException;
import com.biblioteca.auth_service.exception.ResourceNotFoundException;
import com.biblioteca.auth_service.repository.RolRepository;
import com.biblioteca.auth_service.repository.UsuarioRepository;
import com.biblioteca.auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UsuarioResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("El email ya está registrado");
        }

        Rol rolLector = rolRepository.findByNombre("LECTOR")
                .orElseThrow(() -> new ResourceNotFoundException("El rol LECTOR no existe"));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .estado(EstadoUsuario.ACTIVO)
                .rol(rolLector)
                .build();

        return toResponse(usuarioRepository.save(usuario));
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(usuario);
        return new LoginResponse(token, "Bearer", usuario.getEmail(), usuario.getRol().getNombre());
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(),
                u.getEstado().name(), u.getRol().getNombre());
    }
}
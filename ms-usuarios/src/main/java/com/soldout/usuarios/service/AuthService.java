package com.soldout.usuarios.service;

import com.soldout.usuarios.dto.LoginRequest;
import com.soldout.usuarios.dto.LoginResponse;
import com.soldout.usuarios.dto.RegistroRequest;
import com.soldout.usuarios.entity.Usuario;
import com.soldout.usuarios.exception.NegocioException;
import com.soldout.usuarios.repository.UsuarioRepository;
import com.soldout.usuarios.security.JwtUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Set<String> ROLES_VALIDOS = Set.of("CLIENTE", "ORGANIZADOR", "ADMIN");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public Object registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("El email ya esta registrado", "EMAIL_YA_REGISTRADO", HttpStatus.CONFLICT);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        usuario.setTelefono(request.telefono());
        usuario.setRol(obtenerRol(request.rol()));
        usuario.setEstado("ACTIVO");

        return convertirUsuario(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(request.email()))
            .orElseThrow(() -> new NegocioException(
                "Credenciales invalidas",
                "CREDENCIALES_INVALIDAS",
                HttpStatus.UNAUTHORIZED
            ));

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash())) {
            throw new NegocioException("Credenciales invalidas", "CREDENCIALES_INVALIDAS", HttpStatus.UNAUTHORIZED);
        }

        if ("BLOQUEADO".equals(usuario.getEstado())) {
            throw new NegocioException("Usuario bloqueado", "USUARIO_BLOQUEADO", HttpStatus.FORBIDDEN);
        }

        String token = jwtUtil.generarToken(usuario);
        return new LoginResponse(
            token,
            "Bearer",
            jwtUtil.obtenerExpiracion(token),
            convertirUsuarioLogin(usuario)
        );
    }

    @Transactional(readOnly = true)
    public Object obtenerPerfil(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new NegocioException("Token invalido", "TOKEN_INVALIDO", HttpStatus.UNAUTHORIZED);
        }

        Usuario usuario = usuarioRepository.findById(jwtUtil.obtenerUsuarioId(authorization.substring(7)))
            .orElseThrow(() -> new NegocioException("Token invalido", "TOKEN_INVALIDO", HttpStatus.UNAUTHORIZED));

        return convertirUsuario(usuario);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String obtenerRol(String rol) {
        if (rol == null || rol.isBlank()) {
            return "CLIENTE";
        }
        String normalizado = rol.trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(normalizado)) {
            throw new NegocioException("Rol invalido", "SOLICITUD_INVALIDA", HttpStatus.BAD_REQUEST);
        }
        return normalizado;
    }

    private Map<String, Object> convertirUsuario(Usuario usuario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", usuario.getId());
        datos.put("nombre", usuario.getNombre());
        datos.put("email", usuario.getEmail());
        datos.put("telefono", usuario.getTelefono());
        datos.put("rol", usuario.getRol());
        datos.put("estado", usuario.getEstado());
        datos.put("creado_en", usuario.getCreadoEn());
        datos.put("actualizado_en", usuario.getActualizadoEn());
        return datos;
    }

    private Map<String, Object> convertirUsuarioLogin(Usuario usuario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", usuario.getId());
        datos.put("nombre", usuario.getNombre());
        datos.put("email", usuario.getEmail());
        datos.put("rol", usuario.getRol());
        return datos;
    }
}

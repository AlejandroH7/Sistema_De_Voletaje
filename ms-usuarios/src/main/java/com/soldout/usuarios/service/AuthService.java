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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
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
        log.info("LOGIN - Iniciando para email: {}", request.email());
        
        String emailNormalizado = normalizarEmail(request.email());
        log.info("LOGIN - Email normalizado: {}", emailNormalizado);
        
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
            .orElseThrow(() -> {
                log.warn("LOGIN - Usuario no encontrado: {}", emailNormalizado);
                return new NegocioException(
                    "Credenciales invalidas",
                    "CREDENCIALES_INVALIDAS",
                    HttpStatus.UNAUTHORIZED
                );
            });
        
        log.info("LOGIN - Usuario encontrado: {}, estado: {}, rol: {}", 
            usuario.getEmail(), usuario.getEstado(), usuario.getRol());
        
        boolean coincide = passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash());
        log.info("LOGIN - Contraseña coincide: {}", coincide);
        
        if (!coincide) {
            throw new NegocioException("Credenciales invalidas", "CREDENCIALES_INVALIDAS", HttpStatus.UNAUTHORIZED);
        }
        
        if ("BLOQUEADO".equals(usuario.getEstado())) {
            throw new NegocioException("Usuario bloqueado", "USUARIO_BLOQUEADO", HttpStatus.FORBIDDEN);
        }
        
        log.info("LOGIN - Generando token JWT...");
        try {
            String token = jwtUtil.generarToken(usuario);
            log.info("LOGIN - Token generado exitosamente, longitud: {}", token.length());
            return new LoginResponse(
                token,
                "Bearer",
                jwtUtil.obtenerExpiracion(token),
                convertirUsuarioLogin(usuario)
            );
        } catch (Exception e) {
            log.error("LOGIN - Error generando token: {}", e.getMessage(), e);
            throw e;
        }
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

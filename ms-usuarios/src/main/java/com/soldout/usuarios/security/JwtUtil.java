package com.soldout.usuarios.security;

import com.soldout.usuarios.config.JwtConfig;
import com.soldout.usuarios.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final JwtConfig jwtConfig;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plusMillis(jwtConfig.expiracionMs());

        return Jwts.builder()
            .setSubject(usuario.getEmail())
            .claim("usuario_id", usuario.getId().toString())
            .claim("email", usuario.getEmail())
            .claim("nombre", usuario.getNombre())
            .claim("rol", usuario.getRol())
            .setIssuedAt(Date.from(ahora))
            .setExpiration(Date.from(expiracion))
            .signWith(obtenerLlave(), SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean esTokenValido(String token) {
        obtenerClaims(token);
        return true;
    }

    public UUID obtenerUsuarioId(String token) {
        return UUID.fromString(obtenerClaims(token).get("usuario_id", String.class));
    }

    public String obtenerEmail(String token) {
        return obtenerClaims(token).get("email", String.class);
    }

    public String obtenerRol(String token) {
        return obtenerClaims(token).get("rol", String.class);
    }

    public Instant obtenerExpiracion(String token) {
        return obtenerClaims(token).getExpiration().toInstant();
    }

    public Instant calcularExpiracion() {
        return Instant.now().plusMillis(jwtConfig.expiracionMs());
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(obtenerLlave())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key obtenerLlave() {
        String secret = jwtConfig.secret();
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (IllegalArgumentException exception) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
}

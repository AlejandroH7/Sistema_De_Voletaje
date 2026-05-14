package com.soldout.gateway.filter;

import com.soldout.gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ERROR_NO_AUTORIZADO = "{\"exito\":false,\"mensaje\":\"No autorizado\",\"codigo_error\":\"TOKEN_INVALIDO\",\"datos\":null}";
    private static final Set<String> ROLES_GESTION_EVENTOS = Set.of("ORGANIZADOR", "ADMIN");

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        log.info("FILTRO → método: {}, path: {}, esPublica: {}", method, path, esRutaPublica(method, path));

        if (esRutaPublica(method, path)) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return responderNoAutorizado(exchange);
        }

        try {
            Claims claims = jwtUtil.validarToken(authorization.substring(BEARER_PREFIX.length()));
            String usuarioId = claims.get("usuario_id", String.class);
            String rol = claims.get("rol", String.class);
            String email = claims.get("email", String.class);

            if (usuarioId == null || rol == null || email == null || !tienePermiso(method, path, rol)) {
                return responderNoAutorizado(exchange);
            }

            ServerHttpRequest requestConHeaders = request.mutate()
                .header("X-Usuario-Id", usuarioId)
                .header("X-Usuario-Rol", rol)
                .header("X-Usuario-Email", email)
                .build();

            return chain.filter(exchange.mutate().request(requestConHeaders).build());
        } catch (RuntimeException exception) {
            return responderNoAutorizado(exchange);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean esRutaPublica(HttpMethod method, String path) {
        if (HttpMethod.GET.equals(method) && "/actuator/health".equals(path)) {
            return true;
        }
        if (HttpMethod.POST.equals(method) && ("/api/auth/registro".equals(path) || "/api/auth/login".equals(path))) {
            return true;
        }
        if (HttpMethod.GET.equals(method) && ("/api/eventos".equals(path) || path.startsWith("/api/eventos/"))) {
            return true;
        }
        return HttpMethod.GET.equals(method) && ("/api/inventario".equals(path) || path.startsWith("/api/inventario/"));
    }

    private boolean tienePermiso(HttpMethod method, String path, String rol) {
        // ADMIN puede hacer todo
        if ("ADMIN".equals(rol)) {
            return true;
        }

        if (path.startsWith("/api/eventos/") || "/api/eventos".equals(path)) {
            if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method)) {
                return ROLES_GESTION_EVENTOS.contains(rol);
            }
        }

        if (path.startsWith("/api/lugares/") || "/api/lugares".equals(path)) {
            if (HttpMethod.POST.equals(method)) {
                return ROLES_GESTION_EVENTOS.contains(rol);
            }
        }

        if (path.startsWith("/api/reservas/") || "/api/reservas".equals(path)) {
            if (HttpMethod.POST.equals(method)) {
                return "CLIENTE".equals(rol) || "ADMIN".equals(rol);
            }
        }

        if (path.startsWith("/api/pagos/") || "/api/pagos".equals(path)) {
            if (HttpMethod.POST.equals(method)) {
                return "CLIENTE".equals(rol) || "ADMIN".equals(rol);
            }
        }

        return true;
    }

    private Mono<Void> responderNoAutorizado(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ERROR_NO_AUTORIZADO.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}

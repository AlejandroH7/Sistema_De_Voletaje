package com.soldout.usuarios.controller;

import com.soldout.usuarios.dto.LoginRequest;
import com.soldout.usuarios.dto.LoginResponse;
import com.soldout.usuarios.dto.RegistroRequest;
import com.soldout.usuarios.dto.RespuestaApi;
import com.soldout.usuarios.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/registro")
    public ResponseEntity<RespuestaApi<Object>> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.ok(RespuestaApi.exito("Usuario registrado correctamente", authService.registrar(request)));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<RespuestaApi<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(RespuestaApi.exito("Login exitoso", authService.login(request)));
    }

    @GetMapping("/api/usuarios/perfil")
    public ResponseEntity<RespuestaApi<Object>> perfil(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.ok(RespuestaApi.exito("Perfil obtenido correctamente", authService.obtenerPerfil(authorization)));
    }
}

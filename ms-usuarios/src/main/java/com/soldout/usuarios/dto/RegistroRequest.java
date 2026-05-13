package com.soldout.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
    @NotBlank
    @Size(max = 200)
    String nombre,

    @NotBlank
    @Email
    @Size(max = 255)
    String email,

    @NotBlank
    @Size(min = 8, max = 100)
    String contrasena,

    @Size(max = 20)
    String telefono,

    String rol
) {
}

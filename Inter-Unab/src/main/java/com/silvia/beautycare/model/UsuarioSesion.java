package com.silvia.beautycare.model;

import lombok.Value;

/**
 * Copia minima del usuario autenticado que se guarda en la sesion HTTP.
 * No incluye el hash de la contrasena.
 */
@Value
public class UsuarioSesion {

    Long id;
    String email;
    String nombre;

    public static UsuarioSesion de(Usuario u) {
        return new UsuarioSesion(u.getId(), u.getEmail(), u.getNombre());
    }

    public String getNombreVisible() {
        return (nombre == null || nombre.isBlank()) ? email : nombre;
    }
}

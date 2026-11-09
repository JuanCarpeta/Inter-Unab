package com.silvia.beautycare.util;

import com.silvia.beautycare.model.UsuarioSesion;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

/** Acceso al usuario identificado que vive en la sesion HTTP. */
public final class SesionUsuario {

    public static final String ATTR = "usuarioActual";

    private SesionUsuario() {
    }

    public static Optional<UsuarioSesion> actual(HttpSession sesion) {
        if (sesion == null) return Optional.empty();
        Object valor = sesion.getAttribute(ATTR);
        return valor instanceof UsuarioSesion u ? Optional.of(u) : Optional.empty();
    }

    public static void guardar(HttpSession sesion, UsuarioSesion usuario) {
        sesion.setAttribute(ATTR, usuario);
    }

    public static void cerrar(HttpSession sesion) {
        if (sesion != null) sesion.removeAttribute(ATTR);
    }
}

package com.silvia.beautycare.config;

import com.silvia.beautycare.model.UsuarioSesion;
import com.silvia.beautycare.util.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Deja disponible "usuarioActual" en todas las vistas. */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void usuarioActual(Model model, HttpSession sesion) {
        SesionUsuario.actual(sesion)
                .ifPresent((UsuarioSesion u) -> model.addAttribute("usuarioActual", u));
    }
}

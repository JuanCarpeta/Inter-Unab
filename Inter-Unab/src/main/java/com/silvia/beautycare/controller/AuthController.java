package com.silvia.beautycare.controller;

import com.silvia.beautycare.model.Usuario;
import com.silvia.beautycare.model.UsuarioSesion;
import com.silvia.beautycare.service.AuthService;
import com.silvia.beautycare.util.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Registro e inicio de sesion. Solo se admiten correos @unab.edu.co.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @GetMapping("/login")
    public String formularioLogin(HttpSession sesion) {
        if (SesionUsuario.actual(sesion).isPresent()) {
            return "redirect:/items";
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession sesion,
                        RedirectAttributes flash) {

        String correo = AuthService.normalizar(email);

        if (!AuthService.dominioValido(correo)) {
            flash.addFlashAttribute("error",
                    "Solo se aceptan correos institucionales " + AuthService.DOMINIO + ".");
            flash.addFlashAttribute("emailPrevio", email);
            return "redirect:/login";
        }

        if (!auth.existeCuenta(correo)) {
            flash.addFlashAttribute("error",
                    "No existe una cuenta con el correo " + correo + ". Créala primero.");
            flash.addFlashAttribute("sugerirRegistro", true);
            flash.addFlashAttribute("emailPrevio", correo);
            return "redirect:/login";
        }

        Optional<Usuario> usuario = auth.autenticar(correo, password);
        if (usuario.isEmpty()) {
            flash.addFlashAttribute("error", "La contraseña no es correcta. Vuelve a intentarlo.");
            flash.addFlashAttribute("emailPrevio", correo);
            return "redirect:/login";
        }

        SesionUsuario.guardar(sesion, UsuarioSesion.de(usuario.get()));
        flash.addFlashAttribute("mensaje", "Bienvenido/a, " + nombreVisible(usuario.get()) + ".");
        return "redirect:/items";
    }

    @GetMapping("/registro")
    public String formularioRegistro(@RequestParam(required = false) String email,
                                     Model model,
                                     HttpSession sesion) {
        if (SesionUsuario.actual(sesion).isPresent()) {
            return "redirect:/items";
        }
        if (email != null && !email.isBlank()) {
            model.addAttribute("emailPrevio", AuthService.normalizar(email));
        }
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registro(@RequestParam String email,
                           @RequestParam(required = false) String nombre,
                           @RequestParam String password,
                           @RequestParam String password2,
                           HttpSession sesion,
                           RedirectAttributes flash) {

        try {
            Usuario u = auth.registrar(email, nombre, password, password2);
            SesionUsuario.guardar(sesion, UsuarioSesion.de(u));
            flash.addFlashAttribute("mensaje", "Cuenta creada. Bienvenido/a, " + nombreVisible(u) + ".");
            return "redirect:/items";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("emailPrevio", email);
            flash.addFlashAttribute("nombrePrevio", nombre);
            return "redirect:/registro";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession sesion, RedirectAttributes flash) {
        SesionUsuario.cerrar(sesion);
        flash.addFlashAttribute("mensaje", "Sesión cerrada.");
        return "redirect:/login";
    }

    private String nombreVisible(Usuario u) {
        return (u.getNombre() == null || u.getNombre().isBlank()) ? u.getEmail() : u.getNombre();
    }
}

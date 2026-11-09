package com.silvia.beautycare.service;

import com.silvia.beautycare.model.Usuario;
import com.silvia.beautycare.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Registro e inicio de sesion de estudiantes UNAB.
 * Solo se aceptan correos terminados en @unab.edu.co.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String DOMINIO = "@unab.edu.co";
    public static final int LARGO_MINIMO_PASSWORD = 6;

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean dominioValido(String email) {
        return normalizar(email).endsWith(DOMINIO);
    }

    /**
     * Crea la cuenta. Lanza IllegalArgumentException con un mensaje apto para mostrar.
     */
    public Usuario registrar(String emailRaw, String nombre, String password, String password2) {
        String email = normalizar(emailRaw);

        if (email.isEmpty()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }
        if (!dominioValido(email)) {
            throw new IllegalArgumentException("Solo se permiten correos institucionales " + DOMINIO + ".");
        }
        if (password == null || password.length() < LARGO_MINIMO_PASSWORD) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + LARGO_MINIMO_PASSWORD + " caracteres.");
        }
        if (!password.equals(password2)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        if (repo.existeEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
        }

        Usuario u = Usuario.builder()
                .email(email)
                .nombre(nombre == null || nombre.isBlank() ? null : nombre.trim())
                .passwordHash(encoder.encode(password))
                .build();

        repo.crear(u);
        return u;
    }

    /** true si ya hay una cuenta registrada con ese correo. */
    public boolean existeCuenta(String emailRaw) {
        String email = normalizar(emailRaw);
        return !email.isEmpty() && repo.existeEmail(email);
    }

    /** Devuelve el usuario si el correo y la contrasena son correctos. */
    public Optional<Usuario> autenticar(String emailRaw, String password) {
        String email = normalizar(emailRaw);
        if (email.isEmpty() || password == null || password.isEmpty()) {
            return Optional.empty();
        }
        if (!dominioValido(email)) {
            return Optional.empty();
        }

        return repo.buscarPorEmail(email)
                .filter(u -> encoder.matches(password, u.getPasswordHash()));
    }
}

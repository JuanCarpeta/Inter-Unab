package com.silvia.beautycare.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuenta de estudiante UNAB. Solo se admiten correos @unab.edu.co.
 * La contrasena nunca se guarda en claro: se almacena su hash BCrypt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    private Long id;
    private String email;
    private String nombre;
    private String passwordHash;
    private LocalDateTime fechaRegistro;
}

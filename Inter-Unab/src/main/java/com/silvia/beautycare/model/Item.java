package com.silvia.beautycare.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un item disponible para intercambio en la plataforma Inter-Unab.
 * 
 * Con Lombok evitamos escribir manualmente getters, setters, equals, hashCode y toString:
 *   @Data              -> getters + setters + equals/hashCode + toString
 *   @NoArgsConstructor -> new Item() (lo necesita Spring para llenar el formulario)
 *   @AllArgsConstructor-> constructor con todos los campos (lo usa @Builder)
 *   @Builder           -> Item.builder().nombre("...").categoria(Categoria.LIBROS).build()
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "Máximo 80 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "Máximo 255 caracteres")
    private String descripcion;

    private Categoria categoria;
    private Estado estado;
    private LocalDateTime fechaPublicacion;

    /** Correo del estudiante que publicó el item (dueño). */
    private String publicadoPor;

    /**
     * Si el item sigue disponible para intercambio.
     * No es una columna: se calcula desde la tabla intercambio
     * (un item con intercambio ACEPTADO o COMPLETADO queda ocupado).
     */
    @Builder.Default
    private Boolean disponible = true;

    // Fotos del item (se cargan aparte, no vienen directo de la BD en el mapper básico)
    @Builder.Default
    private List<Foto> fotos = new ArrayList<>();
}
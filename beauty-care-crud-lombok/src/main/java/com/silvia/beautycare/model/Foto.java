package com.silvia.beautycare.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Foto asociada a un item para intercambio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Foto {

    private Long id;
    private Long itemId;
    private String nombreArchivo;
    private String rutaArchivo;
    private Boolean esPrincipal;
    private LocalDateTime fechaSubida;
}
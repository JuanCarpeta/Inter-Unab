package com.silvia.beautycare.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una propuesta de intercambio entre dos items: el que se ofrece y el que se pide.
 * No interviene dinero: solo se permutan objetos.
 *
 * Los campos nombreOfrecido / nombreSolicitado no son columnas de la tabla;
 * vienen del JOIN con item para poder mostrarlos en las vistas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intercambio {

    private Long id;

    /** Item que entrega quien propone el intercambio. */
    private Long itemOfrecidoId;

    /** Item que se quiere recibir a cambio. */
    private Long itemSolicitadoId;

    private String mensaje;

    private EstadoIntercambio estado;

    private LocalDateTime fechaPropuesta;
    private LocalDateTime fechaRespuesta;

    // --- Solo para mostrar (JOIN con item) ---
    private String nombreOfrecido;
    private String nombreSolicitado;
    private String duenoOfrecido;
    private String duenoSolicitado;
}

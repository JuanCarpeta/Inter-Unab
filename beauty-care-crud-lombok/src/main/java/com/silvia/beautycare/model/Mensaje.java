package com.silvia.beautycare.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un mensaje del chat asociado a una propuesta de intercambio. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    private Long id;
    private Long intercambioId;
    private String autor;
    private String texto;
    private LocalDateTime fecha;
}

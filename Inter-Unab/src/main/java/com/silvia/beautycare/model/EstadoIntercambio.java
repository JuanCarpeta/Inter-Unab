package com.silvia.beautycare.model;

/**
 * Estados posibles de una propuesta de intercambio.
 *
 * PENDIENTE   -> recien propuesta, esperando respuesta
 * ACEPTADO    -> ambas partes aceptaron; los dos items quedan comprometidos
 * RECHAZADO   -> la propuesta no prospero
 * CANCELADO   -> quien propuso (o acepto) se arrepintio; libera los items
 * COMPLETADO  -> el intercambio se realizo fisicamente
 */
public enum EstadoIntercambio {
    PENDIENTE, ACEPTADO, RECHAZADO, CANCELADO, COMPLETADO
}

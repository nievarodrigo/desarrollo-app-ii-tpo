package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.SalidaModelo;

/** Lo que devuelve el proveedor: la salida estructurada y los tokens que se cobraron (para trazas y costo). */
public record ResultadoModelo(SalidaModelo salida, long tokensEntrada, long tokensSalida) {
    public static ResultadoModelo sinUso(SalidaModelo salida) { return new ResultadoModelo(salida, 0, 0); }
}

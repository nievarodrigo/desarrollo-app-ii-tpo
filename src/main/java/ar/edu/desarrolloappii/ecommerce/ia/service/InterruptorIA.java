package ar.edu.desarrolloappii.ecommerce.ia.service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Plan de apagado (Clase 14, gobernanza): feature flag para desactivar la IA sin romper el sistema.
 * Arranca según {@code GOMARKET_IA_HABILITADA} (por defecto, habilitada) y un ADMIN puede cambiarlo
 * en caliente. Apagada, la búsqueda sigue funcionando con el intérprete de reglas.
 */
public class InterruptorIA {
    private final AtomicBoolean habilitada;

    public InterruptorIA() { this(!"false".equalsIgnoreCase(System.getenv("GOMARKET_IA_HABILITADA"))); }
    public InterruptorIA(boolean inicial) { this.habilitada = new AtomicBoolean(inicial); }

    public boolean habilitada() { return habilitada.get(); }
    public void cambiar(boolean valor) { habilitada.set(valor); }
}

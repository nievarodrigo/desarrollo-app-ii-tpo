package ar.edu.desarrolloappii.ecommerce.pedido.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Últimos mensajes que agotaron sus reintentos y llegaron a la DLQ, para que un ADMIN los revise. */
@ApplicationScoped
public class BuzonMensajesMuertos {
    static final int CAPACIDAD = 200;

    /** Resumen del mensaje muerto. El cuerpo se recorta: es para diagnóstico, no para reprocesar. */
    public record MensajeMuerto(String idMensaje, String colaOrigen, Instant recibido, String cuerpo) { }

    private final Deque<MensajeMuerto> mensajes = new ArrayDeque<>();

    public synchronized void registrar(String idMensaje, String colaOrigen, String cuerpo) {
        String recorte = cuerpo == null ? "" : cuerpo.length() > 500 ? cuerpo.substring(0, 500) + "…" : cuerpo;
        mensajes.addFirst(new MensajeMuerto(idMensaje, colaOrigen, Instant.now(), recorte));
        while (mensajes.size() > CAPACIDAD) mensajes.removeLast();
    }

    public synchronized List<MensajeMuerto> listar() { return List.copyOf(mensajes); }
}

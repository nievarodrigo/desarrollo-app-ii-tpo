package ar.edu.desarrolloappii.ecommerce.pedido.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Consumidor idempotente: recuerda los últimos mensajes ya procesados.
 *
 * JMS garantiza "al menos una vez": tras un rollback o una caída el mismo mensaje
 * puede llegar de nuevo. Si su {@code idMensaje} ya está acá, se ignora sin repetir
 * el efecto. Guarda los últimos 10.000 (LRU) para no crecer sin límite; para los
 * más viejos, la máquina de estados del pedido sigue absorbiendo el duplicado.
 */
@jakarta.enterprise.inject.Vetoed
public class RegistroDeMensajes {
    static final int CAPACIDAD = 10_000;

    private final Set<String> procesados = Collections.newSetFromMap(Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(256, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Boolean> mayor) {
                    return size() > CAPACIDAD;
                }
            }));

    public boolean yaProcesado(String idMensaje) {
        return idMensaje != null && procesados.contains(idMensaje);
    }

    /** Se llama recién DESPUÉS de aplicar el efecto con éxito. */
    public void registrar(String idMensaje) {
        if (idMensaje != null) procesados.add(idMensaje);
    }
}

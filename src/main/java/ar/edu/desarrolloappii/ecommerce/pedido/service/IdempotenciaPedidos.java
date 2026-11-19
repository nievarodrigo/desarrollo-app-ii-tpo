package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.shared.exception.ContenidoNoProcesableException;
import ar.edu.desarrolloappii.ecommerce.shared.exception.DatosInvalidosException;
import ar.edu.desarrolloappii.ecommerce.shared.exception.OperacionEnCursoException;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Idempotency-Key para {@code POST /api/pedidos}.
 *
 * Si el cliente reintenta (timeout, doble clic, red inestable) con la misma clave y el
 * mismo cuerpo, recibe el MISMO pedido y no se crea ni se publica otro. Reglas:
 * <ul>
 *   <li>La clave es por usuario: dos clientes pueden usar el mismo valor sin chocar.</li>
 *   <li>Misma clave + otro cuerpo → 422 (es un error del cliente, no un reintento).</li>
 *   <li>Misma clave mientras la primera sigue en proceso → 409 con Retry-After.</li>
 *   <li>Si la operación falla, la clave se libera para poder reintentar.</li>
 *   <li>Las claves vencen a las 24 h (una "en curso" colgada, a los 5 min).</li>
 *   <li>En una repetición, el controller relee el pedido: el cliente ve su estado ACTUAL.</li>
 * </ul>
 */
@jakarta.enterprise.inject.Vetoed
public class IdempotenciaPedidos {
    static final Duration VIGENCIA = Duration.ofHours(24);
    static final Duration EN_CURSO_MAXIMO = Duration.ofMinutes(5);
    private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9_\\-:.]{8,100}");

    /** Resultado de una ejecución: el pedido y si es una repetición de una anterior. */
    public record Resultado(Pedido pedido, boolean repetido) { }

    private record Entrada(String huella, Pedido pedido, Instant creada) {
        boolean enCurso() { return pedido == null; }
    }

    private final Map<String, Entrada> entradas = new ConcurrentHashMap<>();
    private final Clock reloj;

    public IdempotenciaPedidos() { this(Clock.systemUTC()); }

    /** Para tests: reloj controlable. */
    public IdempotenciaPedidos(Clock reloj) { this.reloj = reloj; }

    public Resultado ejecutar(String clave, String usuario, String huella, Supplier<Pedido> accion) {
        validar(clave);
        limpiarVencidas();
        String id = usuario.toLowerCase() + "|" + clave;
        Entrada nueva = new Entrada(huella, null, reloj.instant());
        Entrada existente = entradas.putIfAbsent(id, nueva);
        if (existente != null) {
            if (!existente.huella().equals(huella)) {
                throw new ContenidoNoProcesableException(
                        "Esa Idempotency-Key ya se usó con otro pedido. Generá una clave nueva para cada compra.");
            }
            if (existente.enCurso()) {
                throw new OperacionEnCursoException("Ya estamos procesando un pedido con esa Idempotency-Key.");
            }
            return new Resultado(existente.pedido(), true);
        }
        boolean exito = false;
        try {
            Pedido pedido = accion.get();
            entradas.put(id, new Entrada(huella, pedido, nueva.creada()));
            exito = true;
            return new Resultado(pedido, false);
        } finally {
            if (!exito) entradas.remove(id, nueva); // cualquier falla (también un Error) libera la clave
        }
    }

    static void validar(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new DatosInvalidosException("Falta la cabecera Idempotency-Key (por ejemplo, un UUID por compra).");
        }
        if (!FORMATO.matcher(clave).matches()) {
            throw new DatosInvalidosException("La Idempotency-Key debe tener entre 8 y 100 caracteres alfanuméricos.");
        }
    }

    private void limpiarVencidas() {
        Instant ahora = reloj.instant();
        Instant limite = ahora.minus(VIGENCIA);
        Instant limiteEnCurso = ahora.minus(EN_CURSO_MAXIMO);
        entradas.entrySet().removeIf(e -> e.getValue().enCurso()
                ? e.getValue().creada().isBefore(limiteEnCurso)   // una operación colgada no bloquea la clave para siempre
                : e.getValue().creada().isBefore(limite));
    }

    int tamanio() { return entradas.size(); }
}

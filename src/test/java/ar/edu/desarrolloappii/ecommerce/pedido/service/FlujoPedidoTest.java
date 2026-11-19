package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.TransicionInvalidaException;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.PedidoEnMemoria;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.AccesoDenegadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.repository.UsuarioEnMemoria;
import ar.edu.desarrolloappii.ecommerce.seguridad.service.AuthService;
import ar.edu.desarrolloappii.ecommerce.seguridad.service.JwtService;
import ar.edu.desarrolloappii.ecommerce.seguridad.service.PasswordHasher;
import ar.edu.desarrolloappii.ecommerce.shared.exception.RecursoNoEncontradoException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Flujo de punta a punta SIN servidor: login → JWT → crear pedido → "cola" → MDB → State.
 * La cola JMS se reemplaza por un publicador falso que le entrega el mensaje JSON al MDB real,
 * así se prueba la misma lógica que corre en WildFly (el broker real se prueba a mano, ver RESUMEN).
 */
class FlujoPedidoTest {
    /** Publicador falso: guarda los mensajes en una lista y se los entrega al MDB cuando se pide. */
    static class ColaEnMemoria implements PublicadorPedidos {
        final java.util.ArrayList<String> mensajes = new java.util.ArrayList<>();
        boolean caida;
        String ultimoId;
        @Override public void publicar(Pedido pedido) {
            ultimoId = pedido.getId();
            if (caida) throw new IllegalStateException("broker caído");
            try (Jsonb jsonb = JsonbBuilder.create()) { mensajes.add(jsonb.toJson(pedido)); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }
    }

    private static final PasswordHasher HASHER = new PasswordHasher();
    private PedidoEnMemoria repositorio;
    private ColaEnMemoria cola;
    private PedidoService servicio;
    private ProcesarPedidoMDB mdb;
    private AuthService auth;
    private JwtService jwt;

    @BeforeEach void preparar() {
        jwt = new JwtService("clave-de-pruebas-con-mas-de-32-bytes!!".getBytes(StandardCharsets.UTF_8),
                Duration.ofMinutes(30), Clock.systemUTC());
        auth = new AuthService(new UsuarioEnMemoria(HASHER), HASHER, jwt);
        repositorio = new PedidoEnMemoria();
        cola = new ColaEnMemoria();
        servicio = new PedidoService(new CatalogoDePrueba(), repositorio, cola);
        mdb = new ProcesarPedidoMDB(repositorio);
    }

    private Identidad ingresar(String email, String clave) {
        return jwt.verificar(auth.ingresar(email, clave).token());
    }

    private static SolicitudPedido unItem() {
        SolicitudPedido s = new SolicitudPedido();
        s.setItems(List.of(new SolicitudPedido.Item(1, 1)));
        return s;
    }

    @Test void flujoFeliz_cliente_compra_la_cola_procesa_y_el_admin_confirma() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        Identidad admin = ingresar("admin@gomarket.com.ar", "admin123");

        Pedido creado = servicio.crear(unItem(), ana);
        assertEquals("PENDIENTE", creado.getEstado());
        assertEquals("cliente@gomarket.com.ar", creado.getCliente());
        assertEquals(1, cola.mensajes.size());

        mdb.procesar(cola.mensajes.get(0)); // el consumidor JMS toma el mensaje
        assertEquals("PROCESADO", servicio.obtener(creado.getId(), ana).getEstado());

        assertEquals("CONFIRMADO", servicio.aplicarEvento(creado.getId(), EventoPedido.CONFIRMAR, admin).getEstado());
    }

    @Test void confirmar_un_pedido_todavia_pendiente_es_conflicto() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        Identidad admin = ingresar("admin@gomarket.com.ar", "admin123");
        Pedido p = servicio.crear(unItem(), ana);
        TransicionInvalidaException e = assertThrows(TransicionInvalidaException.class,
                () -> servicio.aplicarEvento(p.getId(), EventoPedido.CONFIRMAR, admin));
        assertEquals(409, e.status());
        assertEquals("PENDIENTE", servicio.obtener(p.getId(), admin).getEstado(), "el pedido no cambia");
    }

    @Test void cancelar_un_pedido_ya_confirmado_es_conflicto() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        Identidad admin = ingresar("admin@gomarket.com.ar", "admin123");
        Pedido p = servicio.crear(unItem(), ana);
        mdb.procesar(cola.mensajes.get(0));
        servicio.aplicarEvento(p.getId(), EventoPedido.CONFIRMAR, admin);
        assertThrows(TransicionInvalidaException.class,
                () -> servicio.aplicarEvento(p.getId(), EventoPedido.CANCELAR, ana));
        assertEquals("CONFIRMADO", servicio.obtener(p.getId(), ana).getEstado());
    }

    @Test void la_reentrega_del_mismo_mensaje_no_cambia_nada() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        Identidad admin = ingresar("admin@gomarket.com.ar", "admin123");
        Pedido p = servicio.crear(unItem(), ana);
        mdb.procesar(cola.mensajes.get(0));
        servicio.aplicarEvento(p.getId(), EventoPedido.CONFIRMAR, admin);
        mdb.procesar(cola.mensajes.get(0)); // JMS entrega dos veces
        assertEquals("CONFIRMADO", servicio.obtener(p.getId(), ana).getEstado());
    }

    @Test void si_el_cliente_cancela_antes_de_que_llegue_el_mensaje_se_descarta_sin_romper() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        Pedido p = servicio.crear(unItem(), ana);
        servicio.aplicarEvento(p.getId(), EventoPedido.CANCELAR, ana);
        assertDoesNotThrow(() -> mdb.procesar(cola.mensajes.get(0)));
        assertEquals("CANCELADO", servicio.obtener(p.getId(), ana).getEstado());
    }

    @Test void un_mensaje_incompleto_de_un_pedido_inexistente_falla_para_que_termine_en_la_DLQ() {
        assertThrows(IllegalStateException.class,
                () -> mdb.procesar("{\"id\":\"no-existe\",\"estado\":\"PENDIENTE\"}"));
        assertThrows(IllegalStateException.class, () -> mdb.procesar("esto no es json"));
    }

    @Test void tras_un_reinicio_el_mensaje_persistente_reconstruye_el_pedido_y_no_se_pierde() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        Pedido p = servicio.crear(unItem(), ana);
        // Se reinicia el servidor: la memoria se pierde, el journal de Artemis no.
        PedidoEnMemoria repositorioNuevo = new PedidoEnMemoria();
        new ProcesarPedidoMDB(repositorioNuevo).procesar(cola.mensajes.get(0), "msg-1");
        assertEquals("PROCESADO", repositorioNuevo.obtener(p.getId()).getEstado());
        assertEquals(ana.email(), repositorioNuevo.obtener(p.getId()).getCliente());
    }

    @Test void un_mensaje_con_idMensaje_ya_procesado_se_ignora() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        Identidad admin = ingresar("admin@gomarket.com.ar", "admin123");
        Pedido p = servicio.crear(unItem(), ana);
        mdb.procesar(cola.mensajes.get(0), "msg-1");
        servicio.aplicarEvento(p.getId(), EventoPedido.CANCELAR, admin);
        assertDoesNotThrow(() -> mdb.procesar(cola.mensajes.get(0), "msg-1"));
        assertEquals("CANCELADO", servicio.obtener(p.getId(), ana).getEstado());
    }

    @Test void si_el_broker_esta_caido_el_pedido_queda_en_ERROR_ENVIO_y_se_republica_al_volver() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        ReenvioDePedidos reenvio = new ReenvioDePedidos(repositorio, cola);
        cola.caida = true;
        Pedido p = assertDoesNotThrow(() -> servicio.crear(unItem(), ana));
        assertEquals("ERROR_ENVIO", p.getEstado(), "la compra se acepta aunque el broker no responda");

        assertEquals(0, reenvio.reintentarPendientes(), "broker todavía caído");
        assertEquals("ERROR_ENVIO", repositorio.obtener(p.getId()).getEstado());

        cola.caida = false; // vuelve el broker
        assertEquals(1, reenvio.reintentarPendientes());
        assertEquals("PENDIENTE", repositorio.obtener(p.getId()).getEstado());
        mdb.procesar(cola.mensajes.get(0));
        assertEquals("PROCESADO", repositorio.obtener(p.getId()).getEstado());
        assertEquals(0, reenvio.reintentarPendientes(), "nada más para reenviar");
    }

    @Test void un_pedido_cancelado_mientras_el_broker_estaba_caido_no_se_reenvia() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        cola.caida = true;
        Pedido p = servicio.crear(unItem(), ana);
        servicio.aplicarEvento(p.getId(), EventoPedido.CANCELAR, ana);
        cola.caida = false;
        assertEquals(0, new ReenvioDePedidos(repositorio, cola).reintentarPendientes());
        assertTrue(cola.mensajes.isEmpty());
    }

    @Test void un_cliente_no_ve_ni_cancela_pedidos_ajenos_pero_el_admin_si() {
        Identidad ana = ingresar("cliente@gomarket.com.ar", "cliente123");
        Identidad admin = ingresar("admin@gomarket.com.ar", "admin123");
        auth.registrar("Otro Cliente", "otro@correo.com", "clave-larga-1");
        Identidad otro = ingresar("otro@correo.com", "clave-larga-1");

        Pedido p = servicio.crear(unItem(), ana);
        assertThrows(AccesoDenegadoException.class, () -> servicio.obtener(p.getId(), otro));
        assertThrows(AccesoDenegadoException.class,
                () -> servicio.aplicarEvento(p.getId(), EventoPedido.CANCELAR, otro));
        assertEquals(p.getId(), servicio.obtener(p.getId(), admin).getId());
    }

    @Test void un_pedido_inexistente_es_404() {
        Identidad admin = ingresar("admin@gomarket.com.ar", "admin123");
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener("nada", admin));
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.aplicarEvento("nada", EventoPedido.CANCELAR, admin));
    }
}

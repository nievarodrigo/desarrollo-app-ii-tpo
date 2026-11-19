package ar.edu.desarrolloappii.ecommerce.pedido.service;

import ar.edu.desarrolloappii.ecommerce.pedido.domain.EventoPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.Pedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.PedidoConfirmado;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.SolicitudPedido;
import ar.edu.desarrolloappii.ecommerce.pedido.domain.TransicionInvalidaException;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.ClaveIdempotenciaEntity;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.EventoOutboxEntity;
import ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa.PedidoJpa;
import ar.edu.desarrolloappii.ecommerce.shared.exception.ContenidoNoProcesableException;
import ar.edu.desarrolloappii.ecommerce.shared.exception.OperacionEnCursoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifica Repository, Data Mapper, Transactional Outbox e Idempotent Receiver con Flyway, Hibernate y H2.
 * Usa límites RESOURCE_LOCAL para comprobar el Unit of Work sin un contenedor JTA.
 */
class DatosJpaTest {
    static EntityManagerFactory fabrica;
    EntityManager em;
    PedidoJpa pedidos;
    OutboxPedidos outbox;
    RegistroDeMensajesJpa inbox;

    @BeforeAll
    static void esquema() {
        String url = "jdbc:h2:mem:datos;DB_CLOSE_DELAY=-1";
        var flyway = Flyway.configure().dataSource(url, "sa", "").load();
        assertEquals(3, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);
        fabrica = Persistence.createEntityManagerFactory("gomarket-test",
                Map.of("jakarta.persistence.jdbc.url", url,
                        "jakarta.persistence.jdbc.user", "sa",
                        "jakarta.persistence.jdbc.password", ""));
    }

    @AfterAll
    static void cerrar() {
        if (fabrica != null) {
            fabrica.close();
        }
    }

    @BeforeEach
    void preparar() {
        em = fabrica.createEntityManager();
        pedidos = new PedidoJpa(em);
        outbox = new OutboxPedidos(em);
        inbox = new RegistroDeMensajesJpa(em);
    }

    @AfterEach
    void terminar() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        em.close();
    }

    <T> T tx(Supplier<T> accion) {
        em.getTransaction().begin();
        try {
            T valor = accion.get();
            em.getTransaction().commit();
            em.clear();
            return valor;
        } catch (RuntimeException | Error fallo) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.clear();
            throw fallo;
        }
    }

    Pedido nuevo() {
        return new Pedido(UUID.randomUUID().toString(), "PENDIENTE",
                List.of(new SolicitudPedido.Item(7, 2)), new BigDecimal("12345.67"), "ana@x.com");
    }

    long contar(Class<?> tipo) {
        return em.createQuery("select count(e) from " + tipo.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }

    String evento() {
        return em.createQuery("select e.id from EventoOutboxEntity e where e.publicado is null order by e.creado desc", String.class)
                .setMaxResults(1)
                .getSingleResult();
    }

    @Test
    void guardarYLeerPedidoConItemsSinExponerEntidad() {
        var pedido = nuevo();
        tx(() -> {
            pedidos.guardar(pedido);
            return null;
        });
        var leido = tx(() -> pedidos.obtener(pedido.getId()));
        assertEquals(pedido.getEstado(), leido.getEstado());
        assertEquals(pedido.getTotal(), leido.getTotal());
        assertEquals("ana@x.com", leido.getCliente());
        assertEquals(7, leido.getItems().get(0).getIdProducto());
        assertEquals(2, leido.getItems().get(0).getCantidad());
    }

    @Test
    void transicionInvalidaNoModificaEstadoAunqueSeCapture() {
        var pedido = nuevo();
        tx(() -> {
            pedidos.guardar(pedido);
            return null;
        });
        tx(() -> {
            assertThrows(TransicionInvalidaException.class,
                    () -> pedidos.aplicar(pedido.getId(), EventoPedido.CONFIRMAR));
            return null;
        });
        assertEquals("PENDIENTE", tx(() -> pedidos.obtener(pedido.getId())).getEstado());
    }

    @Test
    void transicionValidaPersiste() {
        var pedido = nuevo();
        tx(() -> {
            pedidos.guardar(pedido);
            pedidos.aplicar(pedido.getId(), EventoPedido.MENSAJE_PROCESADO);
            return null;
        });
        assertEquals("PROCESADO", tx(() -> pedidos.obtener(pedido.getId())).getEstado());
    }

    @Test
    void claveDuplicadaViolaUniqueUsuarioClave() {
        String clave = UUID.randomUUID().toString();
        tx(() -> {
            em.persist(clave(clave));
            return null;
        });
        assertThrows(PersistenceException.class, () -> tx(() -> {
            em.persist(clave(clave));
            em.flush();
            return null;
        }));
    }

    ClaveIdempotenciaEntity clave(String clave) {
        var clavePersistida = new ClaveIdempotenciaEntity();
        clavePersistida.id = UUID.randomUUID().toString();
        clavePersistida.usuario = "ana@x.com";
        clavePersistida.clave = clave;
        clavePersistida.huella = "h";
        clavePersistida.creada = Instant.now();
        return clavePersistida;
    }

    @Test
    void pedidoClaveYOutboxSeConfirmanJuntos() {
        var pedido = nuevo();
        var idempotencia = new IdempotenciaPedidosJpa(em, Clock.systemUTC());
        String clave = UUID.randomUUID().toString();
        tx(() -> idempotencia.ejecutar(clave, "ana@x.com", "h", () -> {
            pedidos.guardar(pedido);
            outbox.agregar(pedido);
            return pedido;
        }));
        assertNotNull(tx(() -> pedidos.obtener(pedido.getId())));
        assertEquals(1L, em.createQuery("select count(c) from ClaveIdempotenciaEntity c where c.pedidoId=:id", Long.class)
                .setParameter("id", pedido.getId())
                .getSingleResult());
        assertEquals(1L, em.createQuery("select count(e) from EventoOutboxEntity e where e.payload like :id", Long.class)
                .setParameter("id", "%" + pedido.getId() + "%")
                .getSingleResult());
    }

    @Test
    void falloReviertePedidoClaveYOutboxYLiberaClave() {
        var pedido = nuevo();
        var idempotencia = new IdempotenciaPedidosJpa(em, Clock.systemUTC());
        String clave = UUID.randomUUID().toString();
        long anteriores = contar(EventoOutboxEntity.class);
        assertThrows(IllegalStateException.class,
                () -> tx(() -> idempotencia.ejecutar(clave, "ana@x.com", "h", () -> {
                    pedidos.guardar(pedido);
                    outbox.agregar(pedido);
                    em.flush();
                    throw new IllegalStateException("falla posterior");
                })));
        assertNull(tx(() -> pedidos.obtener(pedido.getId())));
        assertEquals(anteriores, contar(EventoOutboxEntity.class));
        assertFalse(tx(() -> idempotencia.ejecutar(clave, "ana@x.com", "h", () -> {
            pedidos.guardar(pedido);
            outbox.agregar(pedido);
            return pedido;
        })).repetido());
    }

    @Test
    void replayPersistidoYHuellaIncompatible() {
        String clave = UUID.randomUUID().toString();
        var pedido = nuevo();
        tx(() -> new IdempotenciaPedidosJpa(em, Clock.systemUTC()).ejecutar(clave, "ANA@x.com", "h", () -> {
            pedidos.guardar(pedido);
            return pedido;
        }));
        var otro = new IdempotenciaPedidosJpa(em, Clock.systemUTC());
        var resultado = tx(() -> otro.ejecutar(clave, "ana@x.com", "h", () -> {
            fail("no debe crear");
            return null;
        }));
        assertTrue(resultado.repetido());
        assertEquals(pedido.getId(), resultado.pedido().getId());
        assertThrows(ContenidoNoProcesableException.class,
                () -> tx(() -> otro.ejecutar(clave, "ana@x.com", "distinta", () -> null)));
    }

    @Test
    void venceClavePersistidaALas24Horas() {
        String clave = UUID.randomUUID().toString();
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        var pedido = nuevo();
        tx(() -> new IdempotenciaPedidosJpa(em, Clock.fixed(inicio, ZoneOffset.UTC))
                .ejecutar(clave, "ana@x.com", "h", () -> {
                    pedidos.guardar(pedido);
                    return pedido;
                }));
        var otro = nuevo();
        assertFalse(tx(() -> new IdempotenciaPedidosJpa(em, Clock.fixed(inicio.plusSeconds(86401), ZoneOffset.UTC))
                .ejecutar(clave, "ana@x.com", "otra", () -> {
                    pedidos.guardar(otro);
                    return otro;
                })).repetido());
    }

    @Test
    void enCursoPersistidaDa409YVenceALos5Minutos() {
        var clavePersistida = clave(UUID.randomUUID().toString());
        clavePersistida.creada = Instant.parse("2026-01-01T00:00:00Z");
        tx(() -> {
            em.persist(clavePersistida);
            return null;
        });
        assertThrows(OperacionEnCursoException.class,
                () -> tx(() -> new IdempotenciaPedidosJpa(em, Clock.fixed(clavePersistida.creada.plusSeconds(299), ZoneOffset.UTC))
                        .ejecutar(clavePersistida.clave, clavePersistida.usuario, clavePersistida.huella, () -> null)));
        var pedido = nuevo();
        assertFalse(tx(() -> new IdempotenciaPedidosJpa(em, Clock.fixed(clavePersistida.creada.plusSeconds(301), ZoneOffset.UTC))
                .ejecutar(clavePersistida.clave, clavePersistida.usuario, "otra", () -> {
                    pedidos.guardar(pedido);
                    return pedido;
                })).repetido());
    }

    @Test
    void relayReintentaConMismoIdYMarcaPublicado() {
        var pedido = nuevo();
        tx(() -> {
            pedidos.guardar(pedido);
            outbox.agregar(pedido);
            return null;
        });
        String id = evento();
        List<String> ids = new ArrayList<>();
        AtomicInteger llamadas = new AtomicInteger();
        PublicadorPedidos publicador = new PublicadorPedidos() {
            /** Rechaza la publicación sin id para comprobar que el relay conserva la identidad del mensaje. */
            public void publicar(Pedido pedido) {
                fail("el relay debe usar id estable");
            }

            /** Simula una caída inicial para comprobar el reintento con el mismo id de Transactional Outbox. */
            public void publicar(Pedido pedido, String idMensaje) {
                ids.add(idMensaje);
                if (llamadas.getAndIncrement() == 0) {
                    throw new IllegalStateException("broker caído");
                }
            }
        };
        var entrega = new EntregaOutbox(em, publicador, PublicadorDeEventos.NINGUNO);
        assertFalse(tx(() -> entrega.entregar(id)));
        assertNull(em.find(EventoOutboxEntity.class, id).publicado);
        em.clear();
        assertTrue(tx(() -> entrega.entregar(id)));
        assertNotNull(em.find(EventoOutboxEntity.class, id).publicado);
        assertEquals(2, em.find(EventoOutboxEntity.class, id).intentos);
        assertEquals(List.of(id, id), ids);
        assertFalse(tx(() -> entrega.entregar(id)));
        assertEquals(2, llamadas.get());
    }

    @Test
    void confirmacionYEventoSeConfirmanJuntosUnaSolaVez() {
        var pedido = nuevo();
        tx(() -> {
            pedidos.guardar(pedido);
            pedidos.aplicar(pedido.getId(), EventoPedido.MENSAJE_PROCESADO);
            return null;
        });
        var servicio = new PedidoService();
        servicio.repositorio = pedidos;
        servicio.outbox = outbox;
        var admin = new ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad("admin@x.com",
                ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol.ADMIN);
        long antes = contar(EventoOutboxEntity.class);
        tx(() -> servicio.aplicarEvento(pedido.getId(), EventoPedido.CONFIRMAR, admin));
        tx(() -> servicio.aplicarEvento(pedido.getId(), EventoPedido.CONFIRMAR, admin));
        assertEquals(antes + 1, contar(EventoOutboxEntity.class));
        String id = evento();
        AtomicReference<PedidoConfirmado> recibido = new AtomicReference<>();
        assertTrue(tx(() -> new EntregaOutbox(em, pedidoPublicado -> fail("no es cola"), recibido::set).entregar(id)));
        assertEquals(id, recibido.get().idEvento());
        assertEquals(pedido.getId(), recibido.get().idPedido());
    }

    @Test
    void falloConfirmacionRevierteEstadoYEvento() {
        var pedido = nuevo();
        tx(() -> {
            pedidos.guardar(pedido);
            pedidos.aplicar(pedido.getId(), EventoPedido.MENSAJE_PROCESADO);
            return null;
        });
        long antes = contar(EventoOutboxEntity.class);
        assertThrows(IllegalStateException.class, () -> tx(() -> {
            var pedidoConfirmado = pedidos.aplicar(pedido.getId(), EventoPedido.CONFIRMAR);
            outbox.agregar(new PedidoConfirmado(UUID.randomUUID().toString(), PedidoConfirmado.TIPO, 1,
                    Instant.now().toString(), pedidoConfirmado.getId(), pedidoConfirmado.getId(),
                    pedidoConfirmado.getTotal(), "ARS"));
            em.flush();
            throw new IllegalStateException("falla");
        }));
        assertEquals("PROCESADO", tx(() -> pedidos.obtener(pedido.getId())).getEstado());
        assertEquals(antes, contar(EventoOutboxEntity.class));
    }

    @Test
    void inboxPersistenteDescartaDuplicadoYRollbackPermiteReentrega() {
        var pedido = nuevo();
        tx(() -> {
            pedidos.guardar(pedido);
            return null;
        });
        String id = UUID.randomUUID().toString();
        String json;
        try (var serializador = jakarta.json.bind.JsonbBuilder.create()) {
            json = serializador.toJson(pedido);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        var mdb = new ProcesarPedidoMDB(pedidos, inbox);
        tx(() -> {
            mdb.procesar(json, id);
            return null;
        });
        tx(() -> {
            new ProcesarPedidoMDB(pedidos, new RegistroDeMensajesJpa(em)).procesar("incluso JSON inválido", id);
            return null;
        });
        assertEquals("PROCESADO", tx(() -> pedidos.obtener(pedido.getId())).getEstado());
        String fallido = UUID.randomUUID().toString();
        assertThrows(IllegalStateException.class, () -> tx(() -> {
            inbox.registrar(fallido);
            throw new IllegalStateException("falla");
        }));
        assertFalse(inbox.yaProcesado(fallido));
    }

    @Test
    void suscriptoresDeduplicanYPersistenSusEfectos() {
        String id = UUID.randomUUID().toString();
        var evento = new PedidoConfirmado(id, PedidoConfirmado.TIPO, 1, Instant.now().toString(),
                "pedido", "pedido", BigDecimal.TEN, "ARS");
        var consumos = new ConsumosDeEventosJpa(em);
        for (String suscriptor : List.of(ConsumosDeEventos.NOTIFICACIONES, ConsumosDeEventos.AUDITORIA, ConsumosDeEventos.ESTADISTICAS)) {
            assertTrue(tx(() -> consumos.consumir(suscriptor, evento)));
            assertFalse(tx(() -> new ConsumosDeEventosJpa(em).consumir(suscriptor, evento)));
        }
        var resumen = tx(() -> new ConsumosDeEventosJpa(em).resumen());
        assertEquals(1L, resumen.get("notificacionesEnviadas"));
        assertEquals(1L, ((Map<?, ?>) resumen.get("estadisticas")).get("pedidosConfirmados"));
        assertEquals(0, BigDecimal.TEN.compareTo((BigDecimal) ((Map<?, ?>) resumen.get("estadisticas")).get("facturacion")));
    }

    @Test
    void inboxYEstadoSeReviertenJuntos() {
        var pedido = nuevo();
        tx(() -> {
            pedidos.guardar(pedido);
            return null;
        });
        String id = UUID.randomUUID().toString();
        assertThrows(IllegalStateException.class, () -> tx(() -> {
            pedidos.aplicar(pedido.getId(), EventoPedido.MENSAJE_PROCESADO);
            inbox.registrar(id);
            em.flush();
            throw new IllegalStateException("falla");
        }));
        assertEquals("PENDIENTE", tx(() -> pedidos.obtener(pedido.getId())).getEstado());
        assertFalse(inbox.yaProcesado(id));
    }
}

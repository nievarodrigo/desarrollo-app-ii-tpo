# GOMARKET · Matriz de trazabilidad (v2, 3.er checkpoint — 20/11/2026)

Grupo 5 · Desarrollo de Aplicaciones II · Comisión Viernes TM.

Cada requisito de negocio (BR) se sigue hasta la historia de usuario (HU), su criterio de aceptación en Gherkin (CA), el caso de prueba (CP, ver `PLAN_DE_PRUEBAS.md`), el componente y el patrón que lo implementan, y la evidencia. Es la cadena **BR → HU → CA → CP → componente** que pide la Clase 12. Los BR01–BR04 y HU01–HU04 vienen de la entrega de diseño (06/11). En este checkpoint se suman BR05 y HU05–HU07, de integración y resiliencia.

Rutas de clases relativas a `src/main/java/ar/edu/desarrolloappii/ecommerce/`.

## 1. Requisitos de negocio (MoSCoW)

| BR | Requisito | Prioridad | Métrica de aceptación |
|---|---|---|---|
| BR01 | El cliente encuentra productos acordes a su presupuesto combinando búsqueda, categoría, stock, precio y orden | Must | 10/10 búsquedas de prueba coinciden con el catálogo |
| BR02 | El importe que valida el servidor es el que ve el cliente (cantidades, stock y envío) | Must | 100 % de los casos de cantidad y umbral de envío recalculan o rechazan |
| BR03 | El cliente sigue el procesamiento de su pedido con un identificador | Should | 10/10 POST válidos devuelven un id consultable que sigue las transiciones del State |
| BR04 | La búsqueda acepta lenguaje natural con ayuda de IA, sin riesgo para el cliente | Could | Eval set: exactitud ≥ 85 %, adversariales 100 %, formato válido 100 % |
| BR05 | Ninguna compra aceptada se pierde ni se duplica, aunque falle la red, el broker o la IA | Must | Reintento con la misma clave = mismo pedido; kill -9 con mensajes en cola = 0 perdidos; IA caída = búsqueda disponible |

## 2. Historias de usuario y criterios de aceptación

**HU01 · Buscar productos (BR01).** Como visitante, quiero combinar búsqueda, categoría, disponibilidad y precio para encontrar productos acordes a mi presupuesto.
```gherkin
Escenario: Audio disponible dentro del presupuesto
  Dado el catálogo semilla de GOMARKET
  Cuando filtro audio, con stock y precio máximo 300000, ordenado por precio ascendente
  Entonces todos los resultados son de audio, con stock y precio ≤ 300000
  Y sus precios están en orden no decreciente
```

**HU02 · Revisar y confirmar el carrito (BR02).** Como cliente, quiero ver cantidades, envío y total antes de confirmar.
```gherkin
Escenario: Stock excedido
  Dado un producto con stock 8
  Cuando POST /api/v1/pedidos pide 9 unidades
  Entonces responde 400 con application/problem+json
  Y no publica el comando del pedido
```

**HU03 · Consultar el procesamiento (BR03).** Como cliente, quiero consultar el estado del id recibido para saber si se procesó.
```gherkin
Escenario: Mensaje procesado por el MDB
  Dado un pedido aceptado por REST con un id
  Y que el MDB procesó su mensaje
  Cuando consulto GET /api/v1/pedidos/{id}
  Entonces recibo 200 con estado PROCESADO y un ETag
```

**HU04 · Sugerir filtros con IA (BR04).** Como visitante, quiero expresar mi búsqueda con una frase y revisar los filtros sugeridos.
```gherkin
Escenario: Continuidad sin proveedor
  Dado que el proveedor de IA no responde
  Cuando se agota el presupuesto de cinco segundos
  Entonces el gateway responde con el fallback (origen "reglas", motivo "timeout")
  Y siguen disponibles el buscador y los filtros tradicionales
```
*Evolución respecto del diseño:* los filtros sugeridos se aplican como fichas que el cliente puede quitar una por una (humano en el circuito), en lugar de un botón "Aplicar".

**HU05 · Reintentar una compra sin duplicarla (BR05).** Como cliente, quiero poder volver a tocar "Confirmar compra" si se cortó la conexión sin que se cree un segundo pedido.
```gherkin
Escenario: Reintento con la misma clave
  Dado un POST /api/v1/pedidos aceptado con Idempotency-Key K
  Cuando reenvío el mismo cuerpo con la misma clave K
  Entonces recibo 202 con el MISMO id e Idempotent-Replayed: true
  Y no se crea ni se publica otro pedido
```

**HU06 · Confirmar sin pisar cambios ajenos (BR03, BR05).** Como administrador, quiero que la confirmación falle si el pedido cambió desde que lo consulté, y que confirmar avise a las áreas interesadas.
```gherkin
Escenario: Concurrencia optimista
  Dado un pedido que consulté con ETag E
  Y que otro usuario lo modificó después
  Cuando confirmo con If-Match E
  Entonces recibo 412 en Problem Details y el pedido no cambia

Escenario: Aviso por pub/sub
  Dado un pedido PROCESADO
  Cuando lo confirmo
  Entonces se publica un único PedidoConfirmado en PedidoConfirmadoTopic
  Y lo reciben notificaciones, auditoría y estadísticas, cada uno una sola vez
```

**HU07 · Operar la mensajería y la IA (BR04, BR05).** Como administrador, quiero ver los mensajes que fallaron, el estado de la IA y poder apagarla sin redeployar.
```gherkin
Escenario: Mensaje veneno
  Dado un mensaje que el consumidor no puede procesar
  Cuando falla 3 veces
  Entonces va a ProcesarPedidoDLQ y aparece en GET /api/v1/admin/mensajeria/dlq

Escenario: Plan de apagado
  Dado la IA habilitada
  Cuando hago PUT /api/v1/admin/ia/habilitada con {"habilitada": false}
  Entonces la búsqueda sigue funcionando con origen "reglas" y motivo "deshabilitada"
```

Actualización del eje Datos: las evidencias WildFly anteriores corresponden a la implementación en memoria. El flujo JPA/outbox tiene pruebas locales H2; falta repetir la integración en WildFly. Ver ADR-014 y CP-46 a CP-54.

## 3. Matriz BR → HU → CA → CP → componente

| BR | HU | Criterio (CA) | Casos de prueba | Componente | Patrón | Evidencia | Estado |
|---|---|---|---|---|---|---|---|
| BR01 | HU01 | Filtros y orden coinciden | CP-01 a CP-04 | `CatalogoServiceImpl`, `ProductoResource` | Service Layer, Strategy (orden, front), Adapter (catálogo remoto) | `CatalogoServiceImplTest`, `catalogo-service-local.test.mjs` | ✅ |
| BR02 | HU02 | Total del servidor; stock excedido → 400 | CP-06, CP-10, CP-11 | `PedidoService.validarYCalcular`, `carrito-service.js` | Experto en información | `PedidoServiceTest`, `carrito-service.test.mjs` | ✅ |
| BR03 | HU03 | 202 + id; GET con ETag; State | CP-12, CP-13, CP-19 | `PedidoResource`, `ProcesarPedidoMDB`, `EstadosPedido` | State, Idempotent Receiver | `FlujoPedidoTest`, `PedidoStateTest`, `probar-resiliencia.txt` | ✅ |
| BR04 | HU04 | Fallback en 5 s; política; eval set | CP-29 a CP-32, CP-35, CP-42 a CP-44 | `GatewayIA`, `ModeloClaude`, `InterpreteDeReglas`, `CircuitoIA`, `InterruptorIA` | Facade + Adapter, Timeout, Circuit Breaker, Bulkhead, Fallback | `GatewayIATest`, `CircuitoIATest`, `EvalSetIATest`, `ia-admin-y-topico.txt` | ✅ gateway · ⚠️ modelo real sin probar |
| BR05 | HU05 | Misma clave → mismo pedido | CP-14 a CP-18, CP-36 | `IdempotenciaPedidos`, `carrito-view.js` | Idempotency-Key | `IdempotenciaPedidosTest`, `probar-resiliencia.txt` | ✅ |
| BR03, BR05 | HU06 | If-Match viejo → 412; un evento por confirmación | CP-20, CP-39 a CP-41 | `PedidoResource.aplicar`, `PedidoService.anunciarConfirmacion`, `PublicadorDeEventosJms`, 3 MDB suscriptores | Optimistic Locking, Pub/Sub (Observer), Idempotent Receiver | `EventosPedidoTest`, `ia-admin-y-topico.txt` | ✅ |
| BR04, BR05 | HU07 | Veneno → DLQ; apagado en caliente | CP-26, CP-33, CP-43 | `MensajesMuertosMDB`, `MensajeriaResource`, `IAAdminResource` | Dead Letter Channel, Feature Flag | `broker-caido-y-dlq.txt`, `ia-admin-y-topico.txt` | ✅ |
| BR05 | — | Pedido y envío persistidos juntos; reentrega deduplicada | CP-27, CP-28, CP-46 a CP-53 | `PedidoJpa`, `OutboxPedidos`, `RelayOutbox`, `RegistroDeMensajesJpa` | Repository, Data Mapper, Unit of Work, Transactional Outbox, Idempotent Receiver | `DatosJpaTest`; integración WildFly pendiente | ✅ H2 · ⏳ WildFly |

## 4. Detalle por requerimiento funcional y no funcional

| Req. | Descripción | Implementación | Interfaz | Casos de prueba | Tests / evidencia | Estado |
|---|---|---|---|---|---|---|
| RF-01 | Búsqueda por texto | `catalogo/service/CatalogoServiceImpl` | `GET /api/v1/productos?q=` | CP-01, CP-02 | `CatalogoServiceImplTest` | ✅ |
| RF-02 | Filtros y orden | `CatalogoServiceImpl`, `CriteriosDeBusqueda` | `GET /api/v1/productos?categoria=&soloStock=&precioMax=&orden=` | CP-03 | `CatalogoServiceImplTest` | ✅ |
| RF-03 | Facetas y detalle | `CatalogoServiceImpl`, `ProductoResource` | `GET /api/v1/productos/facetas`, `GET /api/v1/productos/{id}` | CP-04, CP-05 | `CatalogoServiceImplTest`, `ProblemasTest`, curl | ✅ |
| RF-04 | Carrito | `carrito-service.js` (front) | UI del carrito | CP-06 | `carrito-service.test.mjs` | ✅ |
| RF-05 | Cuentas y roles | `seguridad/service/AuthService`, `JwtService`, `PasswordHasher` | `POST /api/v1/auth/login`, `/registro`, `GET /api/v1/auth/yo` | CP-07 | `AuthServiceTest`, `JwtServiceTest`, `PasswordHasherTest`, `AuthResourceTest` | ✅ |
| RF-06 | Crear pedido | `pedido/service/PedidoService`, `PedidoResource` | `POST /api/v1/pedidos` (202) | CP-10, CP-11 | `PedidoServiceTest` | ✅ |
| RF-07 | Procesamiento asincrónico (punto a punto) | `PublicadorPedidosJms` → `ProcesarPedidoQueue` → `ProcesarPedidoMDB` | JMS (cola) | CP-12 | `FlujoPedidoTest`, WildFly | ✅ |
| RF-08 | Estados del pedido | `pedido/domain/EstadosPedido` (State) | `GET /api/v1/pedidos/{id}`, `POST …/cancelar`, `POST …/confirmar` | CP-13 | `PedidoStateTest`, `FlujoPedidoTest` | ✅ |
| RF-09 | Búsqueda en lenguaje natural | `ia/service/GatewayIA`, `ModeloClaude`, `InterpreteDeReglas`, `LimiteDeConsultas`; front `main.js › buscarConEnter` | `POST /api/v1/ia/interpretar` | CP-31, CP-32, CP-35 | `GatewayIATest`, `InterpreteDeReglasTest`, `LimiteDeConsultasTest`, curl | ⚠️ modelo real sin probar |
| RF-10 | Revisar la DLQ | `pedido/service/MensajesMuertosMDB`, `BuzonMensajesMuertos`, `MensajeriaResource` | `GET /api/v1/admin/mensajeria/dlq` (ADMIN) | CP-33 | `BuzonMensajesMuertosTest`, curl | ✅ |
| RF-11 | Aviso de pedido confirmado (pub/sub) | `PedidoService.anunciarConfirmacion`, `PublicadorDeEventosJms` → `PedidoConfirmadoTopic` → `NotificacionMDB`, `AuditoriaMDB`, `EstadisticasMDB` (durables) | JMS (tópico); `GET /api/v1/admin/eventos` | CP-39, CP-40, CP-41 | `EventosPedidoTest`, `ia-admin-y-topico.txt` | ✅ |
| RNF-01 | Idempotency-Key | `pedido/service/IdempotenciaPedidos`, `PedidoResource.crear`; front `carrito-view.js` | Cabecera `Idempotency-Key` (400/422/409) | CP-14 a CP-18, CP-36 | `IdempotenciaPedidosTest`, `PedidoResourceTest`, `probar-resiliencia.txt` | ✅ |
| RNF-02 | ETag | `shared/http/Etags`, `Precondiciones`, `ProductoResource.conEtag`, `PedidoResource` | `ETag`, `If-None-Match` (304), `If-Match` (412) | CP-19, CP-20 | `EtagsTest`, `probar-resiliencia.txt` | ✅ |
| RNF-03 | Problem Details | `shared/exception/Problemas`, `ErrorInesperadoMapper`, `ErrorDeNegocioMapper`, `CuerpoInvalidoMapper`; `WEB-INF/web.xml` (JSON-B) | `application/problem+json` en todo error | CP-05, CP-21 a CP-23, CP-37 | `ProblemasTest`, `CuerpoInvalidoMapperTest`, `probar-resiliencia.txt` | ✅ |
| RNF-04 | Consumidor idempotente | `RegistroDeMensajes`, `ProcesarPedidoMDB`, `EstadosPedido`, `ConsumosDeEventos` | Propiedad JMS `idMensaje`; `idEvento` | CP-24, CP-25, CP-40 | `FlujoPedidoTest`, `RegistroDeMensajesTest`, `EventosPedidoTest` | ✅ |
| RNF-05 | DLQ | `scripts/mensajeria.cli` (3 intentos, reentregas a 1 s y 2 s; cola y tópico), `MensajesMuertosMDB` | `ProcesarPedidoDLQ` | CP-26 | `evidencias/broker-caido-y-dlq.txt` | ✅ |
| RNF-06 | Broker caído sin pérdida | `OutboxPedidos`, `RelayOutbox` (timer 5 s), mensajes `PERSISTENT`, inbox persistente y suscriptores durables | `POST /api/v1/admin/mensajeria/reenviar` (forzar) | CP-27, CP-28, CP-41 | `FlujoPedidoTest`, `EventosPedidoTest`, `evidencias/broker-caido-y-dlq.txt` | ✅ |
| RNF-07 | Timeout de 5 s + fallback | `GatewayIA` (`PRESUPUESTO_MS = 5000`, bulkhead), `ModeloClaude` (HTTP 4,5 s, sin reintentos) | `POST /api/v1/ia/interpretar` (`origen`, `motivo`) | CP-29, CP-30 | `GatewayIATest`, WildFly con clave inválida | ✅ (fallback) · ⚠️ (modelo) |
| RNF-08 | Seguridad base | `AutenticacionFilter`, `AutorizadorDeAcceso`, `LimiteDeIntentos`, `PedidoService.visiblePara`; sin secretos en el repo (`.secretos/`) | JWT Bearer, `@RolesAllowed` | CP-07 a CP-09 | `AutorizadorDeAccesoTest`, `FlujoPedidoTest` | ✅ |
| RNF-09 | CI con cobertura y eval set | `.github/workflows/ci.yml`, JaCoCo en `pom.xml`, `scripts/ci/resumen_cobertura.py`, `EvalSetIATest` | GitHub Actions | CP-34, CP-44 | Artefactos `cobertura-java`, `cobertura-js`, `eval-ia` | ⏳ falta el push |
| RNF-10 | Circuit breaker y apagado de la IA | `CircuitoIA` (10 llamadas, 50 %, 30 s), `InterruptorIA`, `IAAdminResource` | `GET /api/v1/admin/ia`, `PUT /api/v1/admin/ia/habilitada` | CP-42, CP-43 | `CircuitoIATest`, `GatewayIATest`, `ia-admin-y-topico.txt` | ✅ |
| RNF-11 | Trazas y costo de la IA | `GatewayIA.interpretar` (log por consulta), `MetricasIA` | Log `ia prompt=… tokens… costoUsd…`; `GET /api/v1/admin/ia` | CP-43 | `GatewayIATest › cadaConsultaSumaTokensYCostoALasMetricas` | ✅ |
| RNF-12 | Versionado y contrato | `config/JaxRsActivator` (`/api/v1`), `docs/openapi.yaml` (OpenAPI 3.1) | Todas las rutas | CP-45 | `probar-resiliencia.txt`, contrato validado | ✅ |

| RNF-13 | Persistencia transaccional con migraciones | `repository/jpa`, `IdempotenciaPedidosJpa`, outbox/inbox, `db/migration`, `scripts/datasource.cli` | JPA/JTA, datasource JNDI externo | CP-46 a CP-54 | `DatosJpaTest`, ADR-014 | ✅ H2 · ⏳ WildFly/PostgreSQL |

## 5. Decisiones de diseño relacionadas

| ADR | Tema | Requerimientos |
|---|---|---|
| ADR-001 a ADR-004 | State, Experto, Strategy, Adapter (diseño) | BR01–BR03 |
| ADR-007 | Mensajería JMS y durabilidad | RF-07, RF-11, RNF-04 a RNF-06 |
| ADR-008 a ADR-010 | Seguridad, State con 409 y puertos | RF-05, RF-08, RNF-08 |
| ADR-011 | Resiliencia de REST y mensajería | RNF-01 a RNF-06 |
| ADR-012 | Gateway de IA y elección del modelo | RF-09, RNF-07, RNF-10, RNF-11 |
| ADR-013 | Pub/sub del pedido confirmado, versionado y por qué no hay SOAP | RF-11, RNF-12 |

| ADR-014 | JPA, Flyway, transacciones locales y outbox/inbox | RNF-01, RNF-04, RNF-06, RNF-13 (D3, D4, D5) |

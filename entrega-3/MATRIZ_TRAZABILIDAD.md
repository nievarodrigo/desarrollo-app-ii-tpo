# GOMARKET · Matriz de trazabilidad (v1, 3.er checkpoint — 20/11/2026)

Cada requerimiento se sigue hasta el código que lo implementa, la interfaz por la que se usa, los casos de prueba que lo verifican (ver `PLAN_DE_PRUEBAS.md`) y la evidencia.

Rutas de clases relativas a `src/main/java/ar/edu/desarrolloappii/ecommerce/`.

| Req. | Descripción | Implementación | Interfaz | Casos de prueba | Tests / evidencia | Estado |
|---|---|---|---|---|---|---|
| RF-01 | Búsqueda por texto | `catalogo/service/CatalogoServiceImpl` | `GET /api/productos?q=` | CP-01, CP-02 | `CatalogoServiceImplTest` | ✅ |
| RF-02 | Filtros y orden | `CatalogoServiceImpl`, `CriteriosDeBusqueda` | `GET /api/productos?categoria=&soloStock=&precioMax=&orden=` | CP-03 | `CatalogoServiceImplTest` | ✅ |
| RF-03 | Facetas y detalle | `CatalogoServiceImpl`, `ProductoResource` | `GET /api/productos/facetas`, `GET /api/productos/{id}` | CP-04, CP-05 | `CatalogoServiceImplTest`, `ProblemasTest`, curl | ✅ |
| RF-04 | Carrito | `carrito-service.js` (front) | UI del carrito | CP-06 | `carrito-service.test.mjs` | ✅ |
| RF-05 | Cuentas y roles | `seguridad/service/AuthService`, `JwtService`, `PasswordHasher` | `POST /api/auth/login`, `/registro`, `GET /api/auth/yo` | CP-07 | `AuthServiceTest`, `JwtServiceTest`, `PasswordHasherTest`, `AuthResourceTest` | ✅ |
| RF-06 | Crear pedido | `pedido/service/PedidoService`, `PedidoResource` | `POST /api/pedidos` (202) | CP-10, CP-11 | `PedidoServiceTest` | ✅ |
| RF-07 | Procesamiento asincrónico | `PublicadorPedidosJms` → `ProcesarPedidoQueue` → `ProcesarPedidoMDB` | JMS (cola P2P) | CP-12 | `FlujoPedidoTest`, WildFly | ✅ |
| RF-08 | Estados del pedido | `pedido/domain/EstadosPedido` (State) | `GET /api/pedidos/{id}`, `POST …/cancelar`, `POST …/confirmar` | CP-13 | `PedidoStateTest`, `FlujoPedidoTest` | ✅ |
| RF-09 | Búsqueda en lenguaje natural | `ia/service/GatewayIA`, `ModeloClaude`, `InterpreteDeReglas`, `LimiteDeConsultas`; front `main.js › buscarConEnter` | `POST /api/ia/interpretar` | CP-31, CP-32, CP-35 | `GatewayIATest`, `InterpreteDeReglasTest`, `LimiteDeConsultasTest`, curl | ⚠️ modelo real sin probar |
| RF-10 | Revisar la DLQ | `pedido/service/MensajesMuertosMDB`, `BuzonMensajesMuertos`, `MensajeriaResource` | `GET /api/admin/mensajeria/dlq` (ADMIN) | CP-33 | `BuzonMensajesMuertosTest`, curl | ✅ |
| RNF-01 | Idempotency-Key | `pedido/service/IdempotenciaPedidos`, `PedidoResource.crear`; front `carrito-view.js` | Cabecera `Idempotency-Key` (400/422/409) | CP-14 a CP-18, CP-36 | `IdempotenciaPedidosTest`, `PedidoResourceTest`, `probar-resiliencia.txt` | ✅ |
| RNF-02 | ETag | `shared/http/Etags`, `Precondiciones`, `ProductoResource.conEtag`, `PedidoResource` | `ETag`, `If-None-Match` (304), `If-Match` (412) | CP-19, CP-20 | `EtagsTest`, `probar-resiliencia.txt` | ✅ |
| RNF-03 | Problem Details | `shared/exception/Problemas`, `ErrorInesperadoMapper`, `ErrorDeNegocioMapper`, `CuerpoInvalidoMapper`; `WEB-INF/web.xml` (JSON-B) | `application/problem+json` en todo error | CP-05, CP-21 a CP-23, CP-37 | `ProblemasTest`, `CuerpoInvalidoMapperTest`, `probar-resiliencia.txt` | ✅ |
| RNF-04 | Consumidor idempotente | `pedido/service/RegistroDeMensajes`, `ProcesarPedidoMDB`, `EstadosPedido` | Propiedad JMS `idMensaje` | CP-24, CP-25 | `FlujoPedidoTest`, `RegistroDeMensajesTest` | ✅ |
| RNF-05 | DLQ | `scripts/mensajeria.cli` (3 intentos, backoff), `MensajesMuertosMDB` | `ProcesarPedidoDLQ` | CP-26 | `evidencias/broker-caido-y-dlq.txt` | ✅ |
| RNF-06 | Broker caído sin pérdida | `PedidoService.crear` (ERROR_ENVIO), `ReenvioDePedidos` (timer 15 s), mensajes `PERSISTENT`, `ProcesarPedidoMDB.rehidratar` | `POST /api/admin/mensajeria/reenviar` (forzar) | CP-27, CP-28 | `FlujoPedidoTest`, `evidencias/broker-caido-y-dlq.txt` | ✅ |
| RNF-07 | Timeout de 5 s + fallback | `GatewayIA` (`PRESUPUESTO_MS = 5000`, bulkhead, validación), `ModeloClaude` (HTTP 4,5 s, sin reintentos) | `POST /api/ia/interpretar` (campos `origen` y `motivo`) | CP-29, CP-30 | `GatewayIATest`, WildFly con clave inválida | ✅ (fallback) · ⚠️ (modelo) |
| RNF-08 | Seguridad base | `AutenticacionFilter`, `AutorizadorDeAcceso`, `LimiteDeIntentos`, `PedidoService.visiblePara` | JWT Bearer, `@RolesAllowed` | CP-07 a CP-09 | `AutorizadorDeAccesoTest`, `FlujoPedidoTest` | ✅ |
| RNF-09 | CI con cobertura | `.github/workflows/ci.yml`, JaCoCo en `pom.xml`, `scripts/ci/resumen_cobertura.py` | GitHub Actions | CP-34 | Artefactos `cobertura-java` y `cobertura-js` | ⏳ falta el push |

## Decisiones de diseño relacionadas

| ADR | Tema | Requerimientos |
|---|---|---|
| ADR-007 | Mensajería JMS y durabilidad | RF-07, RNF-04 a RNF-06 |
| ADR-008 a ADR-010 | Seguridad, State y puertos | RF-05, RF-08, RNF-08 |
| ADR-011 | Resiliencia de REST y mensajería | RNF-01 a RNF-06 |
| ADR-012 | Gateway de IA y elección del modelo | RF-09, RNF-07 |

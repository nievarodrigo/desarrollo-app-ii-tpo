# GOMARKET

**E-commerce integrado sobre Jakarta EE 10.** Catálogo con búsqueda en lenguaje natural por IA, carrito, cuentas con JWT y pedidos que viajan por REST y JMS, con resiliencia de punta a punta. Un único WAR desplegable en WildFly.

Trabajo Práctico Integrador de **Desarrollo de Aplicaciones II** · UADE · Comisión Viernes TM · **Grupo 5**: Rodrigo Nieva, Folco Suarez, Iván Karkoszka, Tomás Ruzak y Nicolás Massimino.

```
Java 17 · Jakarta EE 10 · WildFly 41 · JAX-RS · CDI + EJB · JMS (Artemis) · JUnit 5 · GitHub Actions
```

🔗 [Tienda publicada (modo estático, sin backend)](https://landing-ecommerce-tau.vercel.app) · 📄 [Resumen del 3.er checkpoint](entrega-3/RESUMEN.md) · 📐 [Contrato OpenAPI](docs/openapi.yaml)

---

## Qué hace

| | |
|---|---|
| 🔎 **Catálogo** | Búsqueda sin acentos y con todas las palabras, filtros por categoría, stock y precio, 5 órdenes y facetas |
| 🤖 **Búsqueda con IA** | "auriculares baratos hasta 300 mil" se convierte en filtros. Pasa por un **gateway** con timeout de 5 s, circuit breaker y fallback |
| 🛒 **Carrito y compra** | El total lo recalcula el servidor. La compra es **idempotente**: reintentar no la duplica |
| 📨 **Pedidos asíncronos** | REST responde 202 y el pedido se procesa por una **cola JMS**. Al confirmarlo se avisa por un **tópico** a notificaciones, auditoría y estadísticas |
| 🛡️ **Resiliencia** | Idempotency-Key, ETag (304/412), Problem Details, DLQ, consumidores idempotentes y broker caído sin pérdida |
| 💾 **Persistencia** | JPA/Hibernate sobre un datasource de WildFly, migraciones versionadas con Flyway y **Transactional Outbox**: el pedido y su mensaje se guardan en la misma transacción |
| 👤 **Seguridad** | JWT HS256, roles CLIENTE/ADMIN, contraseñas con PBKDF2 y sal, bloqueo por intentos, cada cliente ve sólo sus pedidos |
| ♿ **Interfaz** | Responsive de 320 a 1440 px, 3 temas, contraste AA y navegación por teclado |

---

## Arranque rápido

**Requisitos:** JDK 17 o superior y WildFly 41 (`wildfly-41.0.1.Final`). No hace falta instalar Maven: el proyecto trae el wrapper.

```bash
# 1. Tests + reporte de cobertura (target/site/jacoco/index.html) + eval set de IA (target/eval-ia.md)
./mvnw verify
node --test src/test/js/*.test.mjs

# 2. Levantar todo: compila, crea el datasource, configura la mensajería y arranca WildFly con JMS
#    (WILDFLY_HOME por defecto: ~/.local/wildfly/wildfly-41.0.1.Final)
./scripts/levantar-wildfly.sh
#    → http://localhost:8080/ecommerce/

# 3. En otra terminal: demo de resiliencia (Idempotency-Key, ETag, Problem Details, IA, DLQ)
./scripts/probar-resiliencia.sh            # --broker agrega la prueba de kill -9
```

**Cuentas de prueba:** `cliente@gomarket.com.ar` / `cliente123` · `admin@gomarket.com.ar` / `admin123`.

| Variable de entorno | Para qué | Si falta |
|---|---|---|
| `GOMARKET_JWT_SECRET` | Firmar los JWT (32+ caracteres) | Se genera una al azar en `.secretos/` (ignorado por git) |
| `ANTHROPIC_API_KEY` | Búsqueda con el modelo de IA real | Responde el modo básico por reglas |
| `GOMARKET_IA_MODELO` | Cambiar el modelo | `claude-haiku-5-5` |
| `GOMARKET_IA_HABILITADA` | Apagar la IA (`false`) | Habilitada |

**Sin secretos en el repositorio:** las claves locales se generan en `.secretos/` y las externas van sólo por variable de entorno.

**Sólo el front, sin backend:** `cd src/main/webapp && python3 -m http.server 4321`. Usa el catálogo estático y simula la compra, rotulada como tal.

---

## Arquitectura

Tres capas por componente (las dependencias apuntan hacia adentro) e integraciones detrás de puertos:

```
 Navegador (HTML/CSS/JS por componentes)
      │  REST /ecommerce/api/v1  ·  JWT  ·  Idempotency-Key  ·  ETag
 ┌────▼──────────────────────────────────────────────────────────────┐
 │ PRESENTACIÓN   JAX-RS: ProductoResource, PedidoResource, AuthResource,
 │                IAResource, admin (IA, mensajería, eventos) · ExceptionMappers
 ├───────────────────────────────────────────────────────────────────┤
 │ NEGOCIO        CatalogoService (EJB) · PedidoService (State) ·
 │                AuthService · GatewayIA (Facade) · IdempotenciaPedidos
 ├───────────────────────────────────────────────────────────────────┤
 │ DATOS          PedidoRepository → PedidoJpa (JPA + Flyway, GomarketDS) · outbox / inbox
 │                ProductoRepository, UsuarioRepository (catálogo JSON y usuarios en memoria)
 └──────┬──────────────────────────┬─────────────────────────┬────────┘
        │ JMS punto a punto        │ JMS pub/sub             │ HTTPS (SDK)
 ProcesarPedidoQueue        PedidoConfirmadoTopic      Proveedor de IA
   → ProcesarPedidoMDB        → Notificacion / Auditoria   (Adapter: ModeloClaude)
   → DLQ tras 3 intentos        / Estadisticas MDB (durables)
```

**Integraciones:** REST (síncrona), JMS con cola y tópico (asíncrona) y el modelo de IA detrás de su gateway. **SOAP no aplica**: GOMARKET es un e-commerce y todos sus consumidores hablan JSON ([ADR-013](docs/adr/adr-013.md)).

### Patrones aplicados

| Patrón | Dónde | Por qué |
|---|---|---|
| **State** | `pedido/domain/EstadosPedido` | Cada estado decide sus transiciones; una inválida es un 409 |
| **Facade + Adapter** | `GatewayIA` + `ModeloIA`/`ModeloClaude` | El negocio nunca llama al SDK; cambiar de proveedor es un adapter nuevo |
| **Strategy / Adapter (front)** | Orden del catálogo; `CatalogoServiceRemoto` vs `Local` | El mismo front funciona con o sin backend |
| **Observer / Pub-Sub** | `PedidoConfirmadoTopic` | Varios módulos reaccionan al mismo evento sin conocerse |
| **Idempotent Receiver** | `ProcesarPedidoMDB`, suscriptores, `IdempotenciaPedidos` | JMS entrega "al menos una vez" y el cliente puede reintentar |
| **Resiliencia** | Timeout 5 s, circuit breaker, bulkhead, fallback, DLQ, reenvío | Ningún fallo externo deja al usuario sin respuesta ni pierde una compra |
| **Repository / Data Mapper / Unit of Work** | `PedidoJpa`, `PedidoMapper`, `EntityManager` | La base se cambia sin tocar el negocio; la API expone DTOs, nunca entidades |
| **Transactional Outbox** | `OutboxPedidos`, `PublicacionInmediata`, `RelayOutbox` | Guardar y publicar en la misma transacción: nunca "guardé pero no publiqué" |
| **Puertos (DIP)** | `PedidoRepository`, `PublicadorPedidos`, `PublicadorDeEventos` | El negocio no depende de la base ni de JMS |

Cada decisión está registrada en [`docs/adr/`](docs/adr/) (ADR-001 a ADR-014).

### Estructura

```text
src/main/java/ar/edu/desarrolloappii/ecommerce/
├── config/          JaxRsActivator (/api/v1), SaludResource
├── catalogo/        controller · service · repository · domain
├── pedido/          controller · service (MDB, publicadores, reenvío, idempotencia) · repository · domain (State, eventos)
├── seguridad/       JWT, roles, hash de contraseñas, límite de intentos
├── ia/              GatewayIA, CircuitoIA, ModeloClaude, InterpreteDeReglas, métricas, apagado
└── shared/          DTO, Problem Details, ETag
src/main/resources/ia/      prompts versionados (busqueda-v1, busqueda-v2)
src/main/resources/db/      migraciones Flyway (V1 pedidos, V2 idempotencia e inbox, V3 outbox)
src/main/webapp/            la tienda (componentes catalogo, carrito, usuario en capas)
src/test/                   JUnit 5 (+ eval set de IA) y node --test
scripts/                    levantar-wildfly.sh, datasource.cli, mensajeria.cli, probar-resiliencia.sh, recorrido-ui.mjs
docs/                       adr/, openapi.yaml, ia/FICHA_IA.md
entrega-3/                  documentos, evidencias, capturas, PDF y presentación del 3.er checkpoint
```

---

## API

Base: **`/ecommerce/api/v1`** · contrato completo en [`docs/openapi.yaml`](docs/openapi.yaml) · colección Postman: [`entrega-3/GOMARKET-checkpoint3.postman_collection.json`](entrega-3/GOMARKET-checkpoint3.postman_collection.json).

| Método y ruta | Quién | Notas |
|---|---|---|
| `GET /salud` | público | Health check |
| `POST /auth/login` · `POST /auth/registro` · `GET /auth/yo` | público / autenticado | JWT de 30 min; 429 tras 5 intentos fallidos |
| `GET /categorias` · `GET /productos` · `/productos/facetas` · `/productos/{id}` | público | ETag + 304 |
| `POST /pedidos` | CLIENTE | **Idempotency-Key obligatoria**; 202 + Location; 422 si se reusa la clave con otro cuerpo |
| `GET /pedidos/{id}` | dueño o ADMIN | ETag + 304 |
| `POST /pedidos/{id}/cancelar` · `/confirmar` | dueño o ADMIN / ADMIN | `If-Match` opcional → 412; transición inválida → 409 |
| `POST /ia/interpretar` | público (30/min por IP) | Filtros sugeridos; nunca falla: si la IA no responde, contesta el fallback |
| `GET /admin/ia` · `PUT /admin/ia/habilitada` | ADMIN | Métricas y apagado en caliente de la IA |
| `GET /admin/mensajeria/dlq` · `POST /admin/mensajeria/reenviar` | ADMIN | Mensajes muertos y reenvío forzado |
| `GET /admin/eventos` | ADMIN | Qué recibió cada suscriptor del tópico |

**Todo error** sale en `application/problem+json` (RFC 9457), sin stack traces ni detalles internos.

---

## Calidad

| | |
|---|---|
| **Tests** | 137 JUnit (negocio, State, idempotencia, ETag, gateway de IA, circuit breaker, pub/sub, JPA con H2 y las mismas migraciones, errores) + 90 del front con `node --test` |
| **Eval set de IA** | 24 casos (8 adversariales) con umbrales; si no se cumplen, el build falla |
| **Cobertura (parcial)** | 68 % de líneas en Java (JaCoCo) y 80 % en JS |
| **CI** | [`.github/workflows/ci.yml`](.github/workflows/ci.yml): tests, cobertura, eval set y WAR como artefactos en cada push y PR |
| **Integración real** | `scripts/probar-resiliencia.sh` contra WildFly; `scripts/recorrido-ui.mjs` recorre la tienda en Chrome y saca capturas |
| **Evidencias** | [`entrega-3/evidencias/`](entrega-3/evidencias/): kill -9 sin pérdida (también con el pedido en la base), DLQ, pub/sub, migraciones, apagado de la IA |

Plan de pruebas (45 casos), matriz de trazabilidad BR → HU → CP y manual de usuario: [`entrega-3/`](entrega-3/).

---

## Límites conocidos

- **Base local:** la demo usa H2 en archivo dentro de WildFly. Pasar a PostgreSQL requiere su driver y el módulo `flyway-database-postgresql` (ADR-014). El catálogo sigue en JSON y los usuarios en memoria.
- **IA:** el gateway y su fallback están verificados; falta correr el eval set contra el modelo real (requiere `ANTHROPIC_API_KEY`).
- Las cuentas creadas sólo en el navegador no existen en el backend; para comprar contra WildFly hay que usar una cuenta del servidor.

---

## Cómo trabajar en el repo

```bash
git switch -c feat/mi-cambio
./mvnw verify && node --test src/test/js/*.test.mjs   # en verde antes de pushear
git push -u origin feat/mi-cambio                      # y Pull Request hacia main
```

Nadie commitea directo a `main`. Commits con [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`).

---

## Documentación y entregas

- **3.er checkpoint (20/11):** [resumen](entrega-3/RESUMEN.md) · [plan de pruebas](entrega-3/PLAN_DE_PRUEBAS.md) · [matriz de trazabilidad](entrega-3/MATRIZ_TRAZABILIDAD.md) · [manual de usuario](entrega-3/MANUAL_DE_USUARIO.md) · [declaración de uso de IA](entrega-3/DECLARACION_USO_IA.md) · [PDF](entrega-3/pdf/) · [guion, plan de demo y guía de defensa](entrega-3/presentacion/)
- **IA:** [ficha de gobernanza](docs/ia/FICHA_IA.md) · [ADR-012](docs/adr/adr-012.md)
- **Diseño (06/11):** [`entrega-diseno/`](entrega-diseno/) — BRD, historias de usuario, contratos y diagramas
- **Entregas anteriores:** [Checkpoint 1](ENTREGA_CHECKPOINT_1.md) · [clase 10 (REST)](entrega-clase10/LEEME.md) · [arquitectura del front](docs/ARQUITECTURA_FRONT.md) · [prototipo Figma](figma-checkpoint-1/README.md)

### Uso de IA generativa

Este proyecto usó asistentes de IA (Claude Code y Codex) para implementar, documentar y revisar, con revisión y pruebas del equipo. El detalle está en la [declaración de uso de IA](entrega-3/DECLARACION_USO_IA.md).

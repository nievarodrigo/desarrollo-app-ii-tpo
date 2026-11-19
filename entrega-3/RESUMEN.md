# GOMARKET · 3.er Checkpoint (20/11) · Integración completa y resiliencia

**Grupo 5** — Rodrigo Nieva, Folco Suarez, Iván Karkoszka, Tomás Ruzak y Nicolás Massimino · Desarrollo de Aplicaciones II · Comisión Viernes TM.

Rama: `entrega-3`. SOAP no aplica: somos un e-commerce, todo en REST/JSON más JMS (cola y tópico); ver ADR-013.
**Tests: 137 de Java + 90 de JavaScript, todos pasan.** Cobertura parcial: 68 % de líneas en Java y 80 % en JS. Eval set de IA: 24 casos con umbrales, en el CI.
Verificado contra **WildFly 41.0.1 real** el 19/11/2026: `kill -9` con mensajes en la cola, DLQ, pub/sub, apagado de la IA y el recorrido completo de la interfaz en Chrome.

## La consigna, punto por punto

| Punto de la consigna | Estado | Qué hay | Dónde mirar |
|---|---|---|---|
| Integraciones funcionando: REST y mensajería | ✅ | REST (JAX-RS, `/api/v1`) + JMS punto a punto (`ProcesarPedidoQueue` → MDB) + JMS pub/sub (`PedidoConfirmadoTopic` → 3 suscriptores durables) + gateway de IA. Compra de punta a punta desde la interfaz | `scripts/probar-resiliencia.sh`, `scripts/recorrido-ui.mjs`, `capturas/` |
| Timeout de 5 s con fallback | ✅ | `GatewayIA`: presupuesto de 5 s, bulkhead de 4, circuit breaker y fallback por reglas | ADR-012, `GatewayIATest`, `CircuitoIATest` |
| Consumidores idempotentes y DLQ | ✅ | `idMensaje`/`idEvento` + registro + State; 3 intentos (reentregas a 1 s y 2 s) → `ProcesarPedidoDLQ`, también para el tópico | ADR-011, `scripts/mensajeria.cli` |
| Si se apaga el broker no se pierden mensajes | ✅ | Mensajes persistentes y suscriptores durables; ERROR_ENVIO y eventos pendientes con reenvío cada 15 s; rehidratación tras reinicio (probado con `kill -9`) | `evidencias/broker-caido-y-dlq.txt` |
| Idempotency-Key y ETag | ✅ | `POST /pedidos` (400/422/409, replay con estado actual); ETag + 304 + 412 en productos y pedidos | `evidencias/probar-resiliencia.txt` |
| Errores en Problem Details | ✅ | Red de seguridad para todo error: 400/401/403/404/405/412/415/422/429/500 | `ProblemasTest`, `docs/openapi.yaml` |
| IA funcionando a través de su gateway | ✅ gateway · ⚠️ modelo | Facade + Adapter, prompt v2 versionado y delimitado, salida validada, circuit breaker, trazas de tokens y costo, feature flag de apagado, límite de uso y eval set en el CI. **El modelo real falta probarlo con una `ANTHROPIC_API_KEY` válida**: sin clave responde el fallback | ADR-012, `docs/ia/FICHA_IA.md`, `evidencias/ia-admin-y-topico.txt` |
| Pipeline de CI con cobertura parcial | ✅ local · ⏳ GitHub | `.github/workflows/ci.yml`: tests de Java y JS, JaCoCo, eval set y WAR como artefactos. **Falta el push** para verlo correr en GitHub | `.github/workflows/ci.yml` |
| Plan de pruebas y matriz de trazabilidad (v1) | ✅ (v2) | 45 casos de prueba; matriz BR → HU (Gherkin) → CP → componente → patrón → evidencia | `PLAN_DE_PRUEBAS.md`, `MATRIZ_TRAZABILIDAD.md` |
| Manual de usuario (v1) | ✅ (v2) | Tutorial con capturas reales, guías por rol, referencia y explicación (Diátaxis) | `MANUAL_DE_USUARIO.md` |

## Persistencia (eje 8 de la rúbrica)

| Criterio | Qué hay | Evidencia |
|---|---|---|
| D3 Migraciones versionadas | Flyway: V1 pedidos, V2 idempotencia e inbox, V3 outbox; corren al arrancar y nunca las genera Hibernate | `src/main/resources/db/migration/`, `evidencias/persistencia-jpa.txt` |
| D4 Transacciones locales | JPA con `@PersistenceContext` y transacciones del contenedor (JTA): clave de idempotencia + pedido + fila del outbox en UNA transacción; si algo falla, no queda ninguna | `DatosJpaTest`, ADR-014 |
| D5 Consistencia entre sistemas | Transactional Outbox (publicación tras el commit + relay cada 5 s) e Idempotent Receiver persistente (inbox) | `DatosJpaTest`, kill -9 con el pedido persistido |

Datasource `java:jboss/datasources/GomarketDS` (H2 en archivo) creado por `scripts/datasource.cli`, nunca en el código.

## Eliminatorias (rúbrica de la cátedra)

| | Estado | Cómo |
|---|---|---|
| E1 App en vivo | ✅ | `scripts/levantar-wildfly.sh` + demo |
| E2 Repo compartido con commits de todos | ⚠️ equipo | Push de `entrega-3` y acceso para el corrector. **Cada integrante debe tener commits propios** |
| E3 Integración síncrona y asíncrona de punta a punta | ✅ | REST + cola + tópico |
| E4 Sin secretos en el repo | ✅ | Clave JWT y usuario JMS generados en `.secretos/` (ignorado por git); API key sólo por variable de entorno |
| E5 Jira al día | ⚠️ equipo | Tarjetas del checkpoint |
| E6 Documentación y presentación antes del horario | ⚠️ equipo | PDF en `entrega-3/` y `entrega-3/presentacion/` |
| E7 Declaración de uso de IA | ✅ | `DECLARACION_USO_IA.md` |
| E8 Defensa individual | ⚠️ equipo | `presentacion/Guia_Defensa_Individual_GOMARKET.pdf` |

## Endpoints nuevos (todos bajo `/ecommerce/api/v1`)

| Método y ruta | Quién | Para qué |
|---|---|---|
| `POST /ia/interpretar` | público (30/min por IP) | Búsqueda en lenguaje natural (200 con modelo o fallback; 429 al superar el límite) |
| `GET /admin/ia` · `PUT /admin/ia/habilitada` | ADMIN | Estado, métricas y apagado de la IA |
| `GET /admin/mensajeria/dlq` · `POST /admin/mensajeria/reenviar` | ADMIN | DLQ y reenvío forzado |
| `GET /admin/eventos` | ADMIN | Qué recibió cada suscriptor del tópico |

Contrato completo: `docs/openapi.yaml` (OpenAPI 3.1). Colección Postman: `GOMARKET-checkpoint3.postman_collection.json`.

## Límites conocidos (decirlos antes de que los pregunten)

- **El modelo de IA real no se probó** (no hay API key en el entorno). Sí se verificó con una clave inválida: el SDK carga, la API responde 401 y el gateway cae al fallback.
- **Base local H2:** suficiente para la demo; pasar a PostgreSQL requiere su driver y el módulo de Flyway correspondiente (ADR-014). Catálogo (JSON) y usuarios siguen en memoria.
- Una cuenta creada sólo en el navegador no existe en el backend: para comprar contra WildFly hay que usar una cuenta del servidor, como las de prueba.

## Documentos de esta entrega

`RESUMEN.md` · `PLAN_DE_PRUEBAS.md` · `MATRIZ_TRAZABILIDAD.md` · `MANUAL_DE_USUARIO.md` · `DECLARACION_USO_IA.md` · `docs/openapi.yaml` · `docs/ia/FICHA_IA.md` · `docs/adr/adr-011.md` a `adr-014.md` · `presentacion/` (guion, plan de demo y guía de defensa).

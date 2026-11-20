# GOMARKET · 3.er Checkpoint (20/11) · Integración completa y resiliencia

**Grupo 5** — Rodrigo Nieva, Folco Suarez, Iván Karkoszka, Tomás Ruzak y Nicolás Massimino · Desarrollo de Aplicaciones II · Comisión Viernes TM.

Rama: `entrega-3`. SOAP no aplica: somos un e-commerce, todo en REST/JSON más JMS (cola y tópico); ver ADR-013.
**Tests: 147 de Java + 90 de JavaScript, todos pasan.** Cobertura parcial: 72 % en Java según JaCoCo (69 % de líneas) y 80 % en JS. Eval set de IA: 24 casos con umbrales, en el CI.
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
| IA funcionando a través de su gateway | ✅ | Facade + Adapter, prompt v3 versionado y delimitado, botón de búsqueda con IA (varita), salida validada, circuit breaker, trazas de tokens y costo, feature flag de apagado, límite de uso y eval set en el CI. **Funciona con un modelo real local** (Ollama, `qwen2.5:3b`; eval: 92 % de exactitud, 100 % de adversariales resistidos y 100 % de formato válido); en producción, Claude Haiku 5.5 con API key. Sin proveedor responde el fallback | ADR-012, `docs/ia/FICHA_IA.md`, `evidencias/eval-ia-modelo-local.txt`, `evidencias/ia-admin-y-topico.txt` |
| Pipeline de CI con cobertura parcial | ✅ local · ⏳ GitHub | `.github/workflows/ci.yml`: tests de Java y JS, JaCoCo, eval set y WAR como artefactos. En GitHub los jobs no arrancan por un **bloqueo de facturación de la cuenta** (repo público, plan gratis, sin deuda), no por el código: el mismo pipeline, `./mvnw verify`, pasa en local y se muestra en vivo | `.github/workflows/ci.yml` |
| Plan de pruebas y matriz de trazabilidad (v1) | ✅ (v2) | 54 casos de prueba más 41 pruebas exploratorias de punta a punta (`INFORME-PRUEBAS.pdf`); matriz BR → HU (Gherkin) → CP → componente → patrón → evidencia | `PLAN_DE_PRUEBAS.md`, `MATRIZ_TRAZABILIDAD.md` |
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
| E2 Repo compartido con commits de todos | ⚠️ equipo | Repo público (github.com/nievarodrigo/desarrollo-app-ii-tpo), rama `entrega-3` pusheada y PR #4 abierto. **Cada integrante debe tener commits propios** antes del merge |
| E3 Integración síncrona y asíncrona de punta a punta | ✅ | REST + cola + tópico |
| E4 Sin secretos en el repo | ✅ | Clave JWT y usuario JMS generados en `.secretos/` (ignorado por git); API key sólo por variable de entorno |
| E5 Tablero al día | ✅ | Trello del equipo (https://trello.com/b/CJeNfB8i) con las tarjetas del checkpoint, responsables y Definition of Done |
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

- **Claude Haiku no se probó contra la API** (no hay API key). El gateway sí se verificó con un modelo real local (Ollama) y con una clave inválida de Anthropic (401 → fallback).
- **Base local H2:** suficiente para la demo; pasar a PostgreSQL requiere su driver y el módulo de Flyway correspondiente (ADR-014). Catálogo (JSON) y usuarios siguen en memoria.
- Una cuenta creada sólo en el navegador no existe en el backend: para comprar contra WildFly hay que usar una cuenta del servidor, como las de prueba.

## Documentos de esta entrega

`RESUMEN.md` · `PLAN_DE_PRUEBAS.md` · `MATRIZ_TRAZABILIDAD.md` · `MANUAL_DE_USUARIO.md` · `DECLARACION_USO_IA.md` · `docs/openapi.yaml` · `docs/ia/FICHA_IA.md` · `docs/adr/adr-011.md` a `adr-014.md` · `presentacion/` (guion, plan de demo y guía de defensa).

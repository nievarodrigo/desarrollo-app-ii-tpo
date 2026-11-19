# GOMARKET · 3.er Checkpoint (20/11) · Integración completa y resiliencia

Rama: `entrega-3` (sale de `entrega-2`). SOAP no aplica: somos un e-commerce, todo en REST/JSON + JMS.
**Tests: 107 de Java + 90 de JavaScript, todos pasan.** Cobertura parcial: 66 % de líneas en Java, 81 % en JS.
Verificado contra **WildFly 41.0.1 real** el 19/11/2026 (incluido un `kill -9` con mensajes en la cola).

| Punto de la consigna | Estado | Qué hay | Dónde mirar |
|---|---|---|---|
| Integraciones funcionando: REST y mensajería | ✅ | REST (JAX-RS) + JMS (cola → MDB) + DLQ con su MDB + gateway de IA. El front ahora pide el JWT al ingresar y compra contra el backend | `scripts/probar-resiliencia.sh`, colección Postman |
| Timeout de 5 s con fallback | ✅ | `GatewayIA`: presupuesto de 5 s, bulkhead de 4, fallback por reglas | ADR-012, `GatewayIATest` |
| Consumidores idempotentes y DLQ | ✅ | `idMensaje` + registro LRU + State; 3 reintentos con backoff → `ProcesarPedidoDLQ` | ADR-011, `scripts/mensajeria.cli` |
| Si se apaga el broker no se pierden mensajes | ✅ | Mensajes persistentes y rehidratación tras reinicio (probado en vivo con `kill -9`); ERROR_ENVIO + reenvío cada 15 s (probado con tests: el broker está embebido en WildFly) | `evidencias/broker-caido-y-dlq.txt` |
| Idempotency-Key y ETag | ✅ | `POST /pedidos` (400/422/409, replay); ETag + 304 + 412 en productos y pedidos | `evidencias/probar-resiliencia.txt` |
| Errores en Problem Details | ✅ | Mapper de red de seguridad: también 404/405/415/JSON mal formado/500 | `ProblemasTest` |
| IA funcionando a través de su gateway | ⚠️ | Gateway, timeout, límite de uso y fallback verificados en WildFly. **El modelo real falta probarlo con una `ANTHROPIC_API_KEY` válida** (con clave inválida: 401 → fallback) | ADR-012 |
| Pipeline de CI con cobertura parcial | ⏳ | `.github/workflows/ci.yml` + JaCoCo; corre localmente. **Falta hacer push** para que corra en GitHub | `.github/workflows/ci.yml` |
| Plan de pruebas y matriz de trazabilidad (v1) | ✅ | 37 casos, 19 requerimientos | `PLAN_DE_PRUEBAS.md`, `MATRIZ_TRAZABILIDAD.md` |
| Manual de usuario (v1) | ✅ | Cliente, admin, mensajes y anexo de puesta en marcha | `MANUAL_DE_USUARIO.md` |

## Correcciones tras la verificación independiente (19/11)

- Bulkhead del gateway: un cupo se perdía si la tarea vencía en la cola del ejecutor.
- Replay de Idempotency-Key: ahora devuelve el estado y el ETag actuales del pedido.
- Un `Error` (no sólo `RuntimeException`) libera la clave; una operación colgada deja de bloquearla a los 5 min.
- Límite de 30 consultas/min por IP en `/api/ia/interpretar` (429).
- `/productos/abc` y `?precioMax=abc` → 400 (antes 404); `If-Match` en un GET → 412 con Problem Details (antes sin cuerpo).
- CI: el paso de JS usa `shell: bash` (pipefail), así un test que falla rompe el job.
- Front: JWT del backend, `Idempotency-Key` sin depender de `crypto.randomUUID` (http sin TLS) y mensaje correcto para ERROR_ENVIO.
- La evidencia del `kill -9` estaba ignorada por `.gitignore` (`*.log`): ahora es `.txt`.

## Endpoints nuevos

| Método y ruta | Quién | Para qué |
|---|---|---|
| `POST /api/ia/interpretar` | público (30/min por IP) | Búsqueda en lenguaje natural (200 con modelo o fallback; 429 si se supera el límite) |
| `GET /api/admin/mensajeria/dlq` | ADMIN | Mensajes que agotaron sus reintentos |
| `POST /api/admin/mensajeria/reenviar` | ADMIN | Forzar el reenvío de pedidos en ERROR_ENVIO |

Cabeceras nuevas: `Idempotency-Key` (obligatoria en `POST /api/pedidos`), `Idempotent-Replayed`, `ETag`, `If-None-Match`, `If-Match`, `Location`.

## Límites conocidos (para decirlos antes de que los pregunten)

- **Compra desde la UI:** al ingresar, el front pide el JWT al backend y lo manda en cada compra. Funciona con las cuentas que existen en el servidor (las de demo); una cuenta creada sólo en el navegador no existe en el backend y no puede comprar contra él. La secuencia HTTP está verificada con curl; falta la prueba visual en el navegador.
- **Sin `ANTHROPIC_API_KEY`** la búsqueda con IA responde con reglas (`origen: "reglas"`). Con clave responde el modelo; no lo pudimos probar con una clave real en esta máquina (sí con una clave inválida: el SDK carga, la API responde 401 y el fallback actúa).
- La idempotencia y el registro de mensajes viven en memoria (como los pedidos): resisten reintentos, no un reinicio. Con JPA pasan a tablas.

## Commits de esta entrega

```
git log --oneline entrega-2..entrega-3
```

# GOMARKET · 3.er Checkpoint (20/11) · Integración completa y resiliencia

Rama: `entrega-3` (sale de `entrega-2`). SOAP no aplica: somos un e-commerce, todo en REST/JSON + JMS.
**Tests: 103 de Java + 90 de JavaScript, todos pasan.** Cobertura parcial: 65 % de líneas en Java, 81 % en JS.
Verificado contra **WildFly 41.0.1 real** el 19/11/2026 (incluido un `kill -9` con mensajes en la cola).

| Punto de la consigna | Estado | Qué hay | Dónde mirar |
|---|---|---|---|
| Integraciones funcionando: REST y mensajería | ✅ | REST (JAX-RS) + JMS (cola → MDB) + DLQ con su MDB + gateway de IA | `scripts/probar-resiliencia.sh` |
| Timeout de 5 s con fallback | ✅ | `GatewayIA`: presupuesto de 5 s, bulkhead de 4, fallback por reglas | ADR-012, `GatewayIATest` |
| Consumidores idempotentes y DLQ | ✅ | `idMensaje` + registro LRU + State; 3 reintentos con backoff → `ProcesarPedidoDLQ` | ADR-011, `scripts/mensajeria.cli` |
| Si se apaga el broker no se pierden mensajes | ✅ | Mensajes persistentes; ERROR_ENVIO + reenvío cada 15 s; rehidratación tras reinicio | `evidencias/broker-caido-y-dlq.log` |
| Idempotency-Key y ETag | ✅ | `POST /pedidos` (400/422/409, replay); ETag + 304 + 412 en productos y pedidos | `evidencias/probar-resiliencia.txt` |
| Errores en Problem Details | ✅ | Mapper de red de seguridad: también 404/405/415/JSON mal formado/500 | `ProblemasTest` |
| IA funcionando a través de su gateway | ✅ | `POST /api/ia/interpretar` + búsqueda en lenguaje natural en el front (Enter) | ADR-012 |
| Pipeline de CI con cobertura parcial | ⏳ | `.github/workflows/ci.yml` + JaCoCo; corre localmente. **Falta hacer push** para que corra en GitHub | `.github/workflows/ci.yml` |
| Plan de pruebas y matriz de trazabilidad (v1) | ✅ | 34 casos, 19 requerimientos | `PLAN_DE_PRUEBAS.md`, `MATRIZ_TRAZABILIDAD.md` |
| Manual de usuario (v1) | ✅ | Cliente, admin, mensajes y anexo de puesta en marcha | `MANUAL_DE_USUARIO.md` |

## Endpoints nuevos

| Método y ruta | Quién | Para qué |
|---|---|---|
| `POST /api/ia/interpretar` | público | Búsqueda en lenguaje natural (siempre responde 200: modelo o fallback) |
| `GET /api/admin/mensajeria/dlq` | ADMIN | Mensajes que agotaron sus reintentos |
| `POST /api/admin/mensajeria/reenviar` | ADMIN | Forzar el reenvío de pedidos en ERROR_ENVIO |

Cabeceras nuevas: `Idempotency-Key` (obligatoria en `POST /api/pedidos`), `Idempotent-Replayed`, `ETag`, `If-None-Match`, `If-Match`, `Location`.

## Límites conocidos (para decirlos antes de que los pregunten)

- **El front no manda el JWT al backend:** la sesión del navegador es local. Con WildFly, la compra desde la UI responde 401; el flujo completo se demuestra con el script, curl o Postman. Es lo próximo a conectar.
- **Sin `ANTHROPIC_API_KEY`** la búsqueda con IA responde con reglas (`origen: "reglas"`). Con clave responde el modelo; no lo pudimos probar con una clave real en esta máquina (sí con una clave inválida: el SDK carga, la API responde 401 y el fallback actúa).
- La idempotencia y el registro de mensajes viven en memoria (como los pedidos): resisten reintentos, no un reinicio. Con JPA pasan a tablas.

## Commits de esta entrega

```
git log --oneline entrega-2..entrega-3
```

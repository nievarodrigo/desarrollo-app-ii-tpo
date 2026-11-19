# GOMARKET · Plan de pruebas (v1, 3.er checkpoint — 20/11/2026)

## 1. Objetivo

Verificar que GOMARKET cumple sus requerimientos funcionales y los de integración y resiliencia del 3.er checkpoint, con evidencia reproducible: tests automáticos en el CI y pruebas de integración contra WildFly real.

## 2. Alcance

| Dentro | Fuera (en esta versión) |
|---|---|
| Catálogo, búsqueda y filtros; carrito; cuentas y roles; pedidos (REST + JMS) | Pagos reales y pasarela externa |
| Resiliencia: Idempotency-Key, ETag, Problem Details, DLQ, broker caído, timeout de 5 s | Persistencia JPA (el repositorio sigue en memoria) |
| Gateway de IA y su fallback | Pruebas de carga y de rendimiento |
| Seguridad base: JWT, roles, dueño del pedido | Pruebas de penetración |

## 3. Requerimientos bajo prueba

| ID | Requerimiento |
|---|---|
| RF-01 | Buscar productos por texto, sin acentos y exigiendo todas las palabras |
| RF-02 | Filtrar por categoría, stock y precio máximo, y ordenar (4 criterios) |
| RF-03 | Ver facetas (conteo por categoría y rango de precios) y el detalle de un producto |
| RF-04 | Gestionar el carrito (alta, baja, cantidades, tope de stock, total) |
| RF-05 | Registrarse e ingresar; sesión con roles CLIENTE / ADMIN |
| RF-06 | Crear un pedido: el servidor recalcula precios y envío y lo acepta con 202 |
| RF-07 | Procesar el pedido de forma asincrónica (cola JMS → MDB) |
| RF-08 | Consultar, cancelar (dueño o ADMIN) y confirmar (ADMIN) un pedido según su estado (State, 409) |
| RF-09 | Buscar en lenguaje natural a través del gateway de IA |
| RF-10 | Un ADMIN puede ver los mensajes que llegaron a la DLQ |
| RNF-01 | Idempotency-Key en la creación de pedidos |
| RNF-02 | ETag con 304 (If-None-Match) y 412 (If-Match) |
| RNF-03 | Todo error de la API en formato Problem Details (RFC 9457) |
| RNF-04 | Consumidor idempotente ante mensajes duplicados |
| RNF-05 | Mensajes que fallan van a una DLQ tras reintentos acotados |
| RNF-06 | Si se apaga el broker no se pierden mensajes ni pedidos |
| RNF-07 | Timeout de 5 s con fallback en la integración con IA |
| RNF-08 | Seguridad: JWT, 401/403, cada cliente ve sólo sus pedidos, contraseñas con hash y sal, bloqueo por intentos |
| RNF-09 | Pipeline de CI con reporte de cobertura |

## 4. Estrategia y niveles

| Nivel | Qué cubre | Herramienta | Cuándo corre |
|---|---|---|---|
| Unitario (backend) | Reglas de negocio, State, idempotencia, ETag, gateway de IA, mappers de error | JUnit 5 (107 tests) | Cada push (CI) y `./mvnw test` |
| Unitario (front) | Carrito, catálogo local, usuario, escape de HTML | `node --test` (90 tests) | Cada push (CI) |
| Flujo sin servidor | Login → JWT → pedido → "cola" → MDB → State, con el publicador reemplazado por una lista | JUnit (`FlujoPedidoTest`) | Cada push (CI) |
| Integración | API REST real, Artemis real, DLQ, `kill -9` del servidor | WildFly 41 + `scripts/probar-resiliencia.sh` + curl | Antes de cada entrega (manual) |
| Exploratorio | Interfaz en el navegador, accesibilidad por teclado | Navegador | Antes de cada demo |

**Técnicas de diseño de casos:** particiones de equivalencia (clave válida, ausente o mal formada), valores límite (1 y 50 ítems, stock exacto, 500 caracteres de consulta, 5 s), transiciones de estado (todas las del State, válidas e inválidas) e inyección de fallas (broker caído, proveedor de IA lento, roto o con salida inventada).

## 5. Entorno

- JDK 17 (Temurin), Maven Wrapper, Node 22.
- WildFly 41.0.1 con `standalone-full.xml` (Artemis embebido), configurado con `scripts/mensajeria.cli`.
- CI: GitHub Actions (`.github/workflows/ci.yml`), Ubuntu, sin servidor.
- IA: sin `ANTHROPIC_API_KEY` (o con una inválida) se prueba el fallback. **Pendiente en esta versión:** la prueba con el modelo real (`claude-haiku-5-5`), que requiere una clave válida.

## 6. Criterios

- **Entrada:** el WAR compila y el CI está verde.
- **Salida:** el 100 % de los tests automáticos pasan; todos los casos de integración de la sección 7 dan el resultado esperado; ningún error de la API sale fuera de Problem Details.
- **Suspensión:** si el WAR no despliega en WildFly, se suspenden las pruebas de integración hasta corregirlo.

## 7. Casos de prueba

Estado al 19/11/2026: ✅ pasa · 🔁 automatizado en el CI · 🖐️ verificado a mano contra WildFly.

| ID | Req. | Caso | Resultado esperado | Cómo | Estado |
|---|---|---|---|---|---|
| CP-01 | RF-01 | Buscar sin acentos ni mayúsculas (p. ej. `cafe` → "Cafetera") | Encuentra el producto | `CatalogoServiceImplTest` | ✅🔁 |
| CP-02 | RF-01 | Buscar dos palabras que no están juntas en ningún producto | Sin resultados (exige todas las palabras) | `CatalogoServiceImplTest` | ✅🔁 |
| CP-03 | RF-02 | Filtrar por categoría, sólo con stock, tope de precio (inclusivo) y ordenar | Lista filtrada y ordenada | `CatalogoServiceImplTest` | ✅🔁 |
| CP-04 | RF-03 | Facetas ignoran el filtro de categoría | Conteo de todas las categorías | `CatalogoServiceImplTest` | ✅🔁 |
| CP-05 | RF-03 | `GET /productos/999` (producto inexistente, desde un EJB) | 404 en Problem Details | `ProblemasTest` + curl | ✅🔁🖐️ |
| CP-06 | RF-04 | Agregar por encima del stock | Rechazado con motivo `tope-stock` | `carrito-service.test.mjs` | ✅🔁 |
| CP-07 | RF-05 | Ingreso correcto / clave incorrecta / 5 fallos | 200 con JWT / 401 / 429 con `Retry-After` | `AuthServiceTest`, `AuthResourceTest` | ✅🔁 |
| CP-08 | RNF-08 | Endpoint protegido sin token / con rol insuficiente | 401 / 403 en Problem Details | `AutorizadorDeAccesoTest` + curl | ✅🔁🖐️ |
| CP-09 | RNF-08 | Cliente consulta el pedido de otro cliente | 403 | `FlujoPedidoTest` | ✅🔁 |
| CP-10 | RF-06 | Pedido con precio manipulado por el cliente | Total recalculado en el servidor | `PedidoServiceTest` | ✅🔁 |
| CP-11 | RF-06 | Pedido vacío, con 51 ítems, repetidos o sin stock | 400 | `PedidoServiceTest` | ✅🔁 |
| CP-12 | RF-07 | Crear pedido → cola → MDB | PENDIENTE → PROCESADO | `FlujoPedidoTest` + WildFly | ✅🔁🖐️ |
| CP-13 | RF-08 | Confirmar un pedido PENDIENTE / cancelar uno CONFIRMADO | 409 en Problem Details | `PedidoStateTest`, `FlujoPedidoTest` | ✅🔁 |
| CP-14 | RNF-01 | `POST /pedidos` sin `Idempotency-Key` | 400 | `IdempotenciaPedidosTest` + curl | ✅🔁🖐️ |
| CP-15 | RNF-01 | Reintento con la misma clave y el mismo cuerpo | 202, mismo id, `Idempotent-Replayed: true`, no se crea otro pedido | `IdempotenciaPedidosTest` + curl | ✅🔁🖐️ |
| CP-16 | RNF-01 | Misma clave con otro cuerpo | 422 | `IdempotenciaPedidosTest` + curl | ✅🔁🖐️ |
| CP-17 | RNF-01 | Misma clave mientras la primera sigue en curso | 409 + `Retry-After` | `IdempotenciaPedidosTest` | ✅🔁 |
| CP-18 | RNF-01 | La operación falla (broker) y se reintenta con la misma clave | Se procesa (la clave se liberó) | `IdempotenciaPedidosTest` | ✅🔁 |
| CP-19 | RNF-02 | `GET` con `If-None-Match` igual al ETag | 304 sin cuerpo | `EtagsTest` + curl (productos y pedidos) | ✅🔁🖐️ |
| CP-20 | RNF-02 | Confirmar con `If-Match` viejo / vigente | 412 / 200 con ETag nuevo | curl | ✅🖐️ |
| CP-21 | RNF-03 | Ruta inexistente, método no permitido, tipo de contenido incorrecto | 404 / 405 / 415 en `application/problem+json` | curl | ✅🖐️ |
| CP-22 | RNF-03 | JSON mal formado | 400 en Problem Details | `CuerpoInvalidoMapperTest` + curl | ✅🔁🖐️ |
| CP-23 | RNF-03 | Error inesperado | 500 genérico sin detalles internos | `ProblemasTest` | ✅🔁 |
| CP-24 | RNF-04 | Mismo mensaje entregado dos veces (mismo `idMensaje`) | El segundo se ignora | `FlujoPedidoTest`, `RegistroDeMensajesTest` | ✅🔁 |
| CP-25 | RNF-04 | Reentrega de un pedido ya CONFIRMADO | Sigue CONFIRMADO | `FlujoPedidoTest` | ✅🔁 |
| CP-26 | RNF-05 | Mensaje inválido en la cola | 3 intentos (reentregas a 1 s y 2 s) y llega a `ProcesarPedidoDLQ`; visible en `/admin/mensajeria/dlq` | WildFly + `EnviarMensajeInvalido.java` | ✅🖐️ |
| CP-27 | RNF-06 | Broker caído al crear el pedido | 202 con estado ERROR_ENVIO; al volver el broker se republica y se procesa | `FlujoPedidoTest` | ✅🔁 |
| CP-28 | RNF-06 | `kill -9` del servidor con un mensaje en la cola | Al reiniciar, el pedido queda PROCESADO | WildFly (`--broker`) | ✅🖐️ |
| CP-29 | RNF-07 | El modelo tarda más que el presupuesto | Se corta la espera; responde el fallback (`motivo: timeout`) | `GatewayIATest` | ✅🔁 |
| CP-30 | RNF-07 | Proveedor caído / salida inventada / sin credenciales / saturado / tareas vencidas en la cola | Fallback (`error`, `salida-invalida`, `sin-proveedor`, `saturado`) y el cupo vuelve | `GatewayIATest` + WildFly con clave inválida | ✅🔁🖐️ |
| CP-31 | RF-09 | "auriculares baratos hasta 50 mil" | categoría audio, orden precio-asc, precioMax 50000 | `InterpreteDeReglasTest` + curl | ✅🔁🖐️ |
| CP-32 | RF-09 | Intento de inyección o consulta con un email | `rechazada` / `aclarar`, sin llamar al modelo | `GatewayIATest` | ✅🔁 |
| CP-33 | RF-10 | `GET /admin/mensajeria/dlq` como CLIENTE / ADMIN | 403 / lista de mensajes | curl | ✅🖐️ |
| CP-34 | RNF-09 | Push a la rama | El CI corre tests y publica la cobertura | GitHub Actions | ⏳ pendiente del push |
| CP-35 | RF-09 | 31 consultas de IA en un minuto desde la misma IP | La 31.ª da 429 con `Retry-After`; otra IP no se ve afectada | `LimiteDeConsultasTest` + curl | ✅🔁🖐️ |
| CP-36 | RNF-01 | Reintento después de que el pedido se procesó | El replay devuelve el estado ACTUAL (PROCESADO) | curl | ✅🖐️ |
| CP-37 | RNF-03 | `/productos/abc`, `?precioMax=abc`, GET con `If-Match` viejo | 400 / 400 / 412, todos en Problem Details | curl | ✅🖐️ |
| CP-38 | RF-06 | Comprar desde la interfaz con la cuenta de demo y WildFly | Pedido aceptado (el front manda JWT e Idempotency-Key) | Navegador | ⏳ pendiente (secuencia HTTP verificada con curl) |

## 8. Cobertura (parcial)

`./mvnw verify` genera `target/site/jacoco/index.html`. Al 19/11: **66 % de líneas y 61 % de ramas en Java**, **81 % de líneas en JavaScript**. Los paquetes `controller` aparecen bajos porque los recursos JAX-RS se prueban contra WildFly (CP-05, CP-14 a CP-22) y no con tests unitarios; es la próxima mejora (tests de recursos con un cliente JAX-RS embebido).

## 9. Riesgos

| Riesgo | Mitigación |
|---|---|
| El repositorio en memoria pierde datos en un reinicio | Rehidratación desde el mensaje persistente (CP-28); JPA en la próxima iteración |
| Las pruebas de integración son manuales | Script `probar-resiliencia.sh` repetible; evidencias en `entrega-3/evidencias/` |
| Costo y disponibilidad del proveedor de IA | Gateway con fallback gratuito; modelo más barato (ADR-012) |

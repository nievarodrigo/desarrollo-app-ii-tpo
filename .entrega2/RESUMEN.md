# GOMARKET · 2.ª Entrega Parcial (13/11) · Resumen

Rama: `entrega-2` (sin push, `main` intacta). Todo en REST/JSON, nada de SOAP.
**Tests: 69 de Java + 90 de JavaScript, todos pasan.** La revisión previa informó un flujo completo contra WildFly 41 real. En esta revisión no pudo repetirse: el sandbox impide crear sockets (ver sección 6).

## 1. Qué quedó hecho por cada punto de la consigna

| Punto | Estado | Qué hay |
|---|---|---|
| 1. Capas desplegadas y flujo de punta a punta | ✅ | El WAR (`ecommerce.war`) despliega en WildFly. Capas `controller → service → repository` en `catalogo`, `pedido` y `seguridad`. Flujo: login → pedido (REST) → cola JMS → MDB → consulta → confirmación. Ahora el service y el MDB dependen de interfaces (`PedidoRepository`, `PublicadorPedidos`). |
| 2. Patrones; State con 409 | ✅ | **State** en `Pedido` (`EstadoPedido`, `EstadosPedido`, `EventoPedido`). Una transición inválida lanza `TransicionInvalidaException` y la API responde **409** (Problem Details). También: Strategy (orden del catálogo) y Adapter (catálogo remoto) del front, y los puertos/adaptadores del backend. |
| 3. Seguridad base | ✅ | **JWT HS256** (login, 30 min), **roles CLIENTE/ADMIN** con `@RolesAllowed` (401 sin token, 403 con rol insuficiente), claves con **PBKDF2 + sal**, bloqueo a los 5 fallos (429), registro siempre CLIENTE, cada cliente ve solo sus pedidos. |
| 4. Mensajería | ✅ | `PublicadorPedidosJms` manda el pedido a `ProcesarPedidoQueue` y `ProcesarPedidoMDB` lo pasa de PENDIENTE a PROCESADO. Verificado en WildFly (`standalone-full.xml`). Los duplicados no hacen daño y un mensaje de un pedido cancelado se descarta. |
| 5. Primeros tests unitarios | ✅ | 69 tests de Java (ver sección 3). |
| 6. Jira | 📝 | No lo tocamos: las tarjetas sugeridas están en `.entrega2/JIRA.md`. |
| 7. Feedback del diseño | ✅/⚠️ | Ver sección 4. |

### Estados y transiciones del pedido

```
PENDIENTE_VALIDACION → (reserva aceptada) PENDIENTE | (rechazada) RECHAZADO
PENDIENTE   → (mensaje procesado) PROCESADO | (envío falló) ERROR_ENVIO | (cancelar) CANCELADO
ERROR_ENVIO → (reintentar) PENDIENTE | (mensaje tardío) PROCESADO | (cancelar) CANCELADO
PROCESADO   → (confirmar) CONFIRMADO | (cancelar) CANCELADO
CONFIRMADO → confirmar o mensaje duplicado: sin cambios; otros eventos = 409
RECHAZADO, CANCELADO → finales (eventos de negocio = 409)
```

### Endpoints

| Método y ruta | Quién | Respuestas |
|---|---|---|
| `POST /api/auth/login`, `POST /api/auth/registro` | público | 200/201, 400, 401, 409, 429 |
| `GET /api/auth/yo` | CLIENTE o ADMIN | 200, 401 |
| `GET /api/auth/usuarios` | ADMIN | 200, 401, 403 |
| `GET /api/productos…` | público | 200 |
| `POST /api/pedidos` | CLIENTE | 202, 400, 401, 403 |
| `GET /api/pedidos/{id}` | dueño o ADMIN | 200, 403, 404 |
| `POST /api/pedidos/{id}/cancelar` | dueño o ADMIN | 200, 403, 404, **409** |
| `POST /api/pedidos/{id}/confirmar` | ADMIN | 200, 403, 404, **409** |

## 2. Cómo probarlo

**Requisitos sin sudo:** un JDK 17+ (usamos Temurin 17 en `~/.local/jdk`) y WildFly 41 descomprimido en `~/.local/wildfly/wildfly-41.0.1.Final` (las instrucciones para bajarlo están en el encabezado de `scripts/levantar-wildfly.sh`).

```bash
# a) Tests (no necesitan servidor)
export JAVA_HOME=~/.local/jdk PATH=~/.local/jdk/bin:$PATH
./mvnw test

# b) Levantar el servidor (compila, despliega y arranca con JMS)
./scripts/levantar-wildfly.sh          # dejarlo corriendo en una terminal

# c) En otra terminal: recorrido completo automático
./scripts/probar-entrega2.sh
```

Y a mano con curl:

```bash
B=http://localhost:8080/ecommerce/api

# Login (cuentas de demo: cliente@gomarket.com.ar/cliente123 y admin@gomarket.com.ar/admin123)
CT=$(curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"cliente@gomarket.com.ar","clave":"cliente123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
AT=$(curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@gomarket.com.ar","clave":"admin123"}'   | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# Sin token -> 401        |  Cliente en ruta de admin -> 403
curl -i -X POST $B/pedidos -H 'Content-Type: application/json' -d '{"items":[{"idProducto":1,"cantidad":1}]}'
curl -i $B/auth/usuarios -H "Authorization: Bearer $CT"

# Crear pedido (202, queda PENDIENTE y la cola lo pasa a PROCESADO)
curl -s -X POST $B/pedidos -H "Authorization: Bearer $CT" -H 'Content-Type: application/json' \
  -d '{"items":[{"idProducto":1,"cantidad":1}]}'
ID=<el id que devolvió>
curl -s $B/pedidos/$ID -H "Authorization: Bearer $CT"            # estado: PROCESADO

# Confirmar (solo admin) y después intentar cancelar -> 409
curl -s -X POST $B/pedidos/$ID/confirmar -H "Authorization: Bearer $AT"
curl -i -X POST $B/pedidos/$ID/cancelar  -H "Authorization: Bearer $CT"   # HTTP 409
```

La respuesta 409 es así:
`{"title":"Transición de estado inválida","status":409,"detail":"No se puede aplicar CANCELAR a un pedido en estado CONFIRMADO.",...}`

Clave de firma del JWT: variable `GOMARKET_JWT_SECRET` (32+ caracteres). Si no existe, el script usa una de demo.

## 3. Qué tests hay (69 Java)

| Clase | Tests | Qué prueba |
|---|---|---|
| `PedidoStateTest` | 11 | Transiciones válidas e inválidas, estados finales, reentrega, cancelar, el 409 y que se conserve el dueño. |
| `FlujoPedidoTest` | 9 | Recorrido login → JWT → pedido → cola → MDB → confirmar; pedido ajeno (403); inexistente (404); cola caída (ERROR_ENVIO); cancelado antes de procesarse. La cola se reemplaza por una lista en memoria. |
| `JwtServiceTest` | 9 | Ida y vuelta, rol editado (firma rota), otra clave, vencido, `alg=none`, basura. |
| `AuthServiceTest` | 7 | Login, mismo error exista o no el correo, bloqueo a los 5 fallos y liberación a los 15 min, registro siempre CLIENTE, duplicados. |
| `AutorizadorDeAccesoTest` | 9 | 401 sin token o token falso, 403 por rol, anotaciones de clase y método, público. |
| `PasswordHasherTest` | 3 | Hash con sal distinta, verificación, hash roto. |
| `PedidoServiceTest`, `CatalogoServiceImplTest`, `CuerpoInvalidoMapperTest` | 5 + 12 + 2 | Total, stock, catálogo, JSON roto y entrega JMS anterior a una falla de publicación. |
| `AuthResourceTest` | 2 | Login y registro sin cuerpo devuelven errores de negocio 400. |

Más 90 tests de JavaScript del front (carrito 28, catálogo 25, usuarios 35 y vista de usuario 2): `node src/test/js/<archivo>.test.mjs`.

**Lo que NO prueban los tests unitarios:** el broker JMS ni el HTTP real. El script `scripts/probar-entrega2.sh` permite probarlo contra WildFly; la ejecución anterior fue informada como exitosa. En esta revisión el arranque quedó bloqueado por los permisos de sockets del entorno.

## 4. Feedback del diseño: qué incorporamos y qué asumimos

- **Feedback concreto del profe que encontramos** (devolución del TP Inicial, en `docs/GUIA_DE_ESTUDIO.md`): *"Algunos grupos mezclan componente con pantalla"*. Ya estaba cubierto: cada componente tiene su interfaz y sus tres capas; ahora `pedido` y `seguridad` siguen el mismo esquema.
- **Clase 12 (patrones):** State en el pedido (ADR-001) y la violación SOLID-D (el service dependía de `PedidoEnMemoria`) quedó corregida con `PedidoRepository`/`PublicadorPedidos` (ADR-010). Diferencia: la clase proponía métodos `procesar()`/`falloEnvio()`; nosotros usamos un único `aplicar(EventoPedido)`, que escala mejor a más eventos.
- **Clase 11 (seguridad):** quedan cubiertos del lado servidor SEC-01 (rol verificado en el servidor), SEC-02, SEC-03, SEC-04, SEC-09 y SEC-10 (el listado sin hash). **SEC-06 corregido en esta revisión:** el menú y el padrón escapan nombres, correos y atributos al generar HTML. SEC-07 (timeout del cliente HTTP) sigue pendiente en el front.
- **Diseño de integración (SOAP):** el PDF de diseño y `entrega-diseno/contratos` planteaban un adaptador SOAP al "inventario legado". En `src/` no había nada SOAP, y **no se implementó**: decidimos REST/JSON (ADR-009). Esos archivos no se borraron.
- **Estado `PENDIENTE_VALIDACION`/`RECHAZADO`:** existen en State pero ninguna ruta los usa todavía (era para la reserva con el ERP).

**Asumimos:**
1. No encontramos otro feedback textual del profe sobre el diseño; implementamos lo que pide la consigna, el PDF de diseño y las clases 11 y 12.
2. Las cuentas de demo (cliente y admin) son las mismas del front, solo para desarrollo.
3. `PBKDF2` en lugar de Argon2id (Argon2 no viene en la JDK; OWASP acepta PBKDF2 con 600.000 iteraciones).
4. Agregamos `CANCELADO` y las rutas `cancelar`/`confirmar` para poder mostrar el 409 de forma repetible por HTTP.
5. Un pedido ajeno devuelve 403 (no 404) para que sea fácil de explicar.
6. Compilamos con `release 17`; el JDK de la máquina es Temurin 17 (el enunciado hablaba de 21, pero no hace falta).

## 5. Qué falta

- El front todavía usa su sesión local; falta conectarlo al login con JWT.
- Usuarios y pedidos están en memoria (se pierden al reiniciar); no hay refresh ni revocación de tokens.
- Outbox/inbox (ADR-007) sigue siendo diseño.
- Los PDF de la entrega de diseño y su OpenAPI no se actualizaron (siguen diciendo "sin autenticación").
- Jira: cargar las tarjetas de `JIRA.md` (lo hace el equipo).
- Se revirtió únicamente la línea `.entrega2/` agregada a `.gitignore`: el material vuelve a ser visible para git. `entrega-diseno/` y `guion-defensa-final/` siguen sin commitear, como estaban.

## Commits en `entrega-2`

1. `feat(pedido): aplicar el patrón State al pedido y documentar los ADR` (tu trabajo previo)
2. `feat(pedido): devolver 409 ante una transición inválida y sumar el estado CANCELADO`
3. `feat(seguridad): autenticación con JWT HS256 y control de acceso por rol`
4. `refactor(pedido): puertos de repositorio y mensajería, pedidos protegidos por rol y dueño`
5. `fix(seguridad): usar @ApplicationScoped … y sumar scripts y ADR 008-010`


## 6. Revisión independiente del 12/11/2026

Se contrastaron `.entrega2/PROMPT.md`, el código Java/JS, los ADR, el informe de diseño, sus contratos y las clases 11 y 12. No se agregaron refactors de estilo ni SOAP.

**Problemas corregidos:**
- La inicialización CDI aceptaba un secreto JWT corto aunque el constructor de tests lo rechazaba. Ambos caminos ahora exigen al menos 32 bytes. Una configuración inválida impide iniciar el servicio.
- Registro inválido y login/registro sin cuerpo lanzaban excepciones sin mapper y podían terminar en 500. Ahora usan `DatosInvalidosException`, traducida a 400 por el mapper de negocio.
- El autorizador ignoraba `@DenyAll` de clase. Ahora aplica la regla del método cuando existe, y en caso contrario la de clase. También acepta `bearer` en minúsculas.
- Si el MDB procesaba el mensaje antes de un error ambiguo del publicador, intentar marcar `ERROR_ENVIO` lanzaba un 409 interno que ocultaba la falla original. Ahora conserva `PROCESADO` y propaga la falla de publicación, con la transición rechazada como excepción suprimida.
- SEC-06: nombres con etiquetas HTML se interpretaban en el menú y el listado de usuarios. Se escapan los datos al componer HTML, incluidas las comillas de los atributos. Dos regresiones prueban las vistas usando el servicio real y un documento simulado.

**Verificación de esta revisión:**
```bash
export JAVA_HOME=/home/rodrigonieva/.local/jdk && ./mvnw test
# Tests run: 69, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
node src/test/js/carrito-service.test.mjs         # 28
node src/test/js/catalogo-service-local.test.mjs  # 25
node src/test/js/usuario-service.test.mjs         # 35
node src/test/js/usuario-view.test.mjs            # 2
```
También compiló el WAR con `./mvnw -q -DskipTests package`. El PDF actualizado fue regenerado con Python/Cairo/Pango sin sudo: 6 páginas, abre con las herramientas PDF y tiene texto extraíble. Se intentó iniciar WildFly desde una base aislada en `/tmp/gomarket-revision-wf`: no pudo abrir sockets (`java.net.SocketException: Operation not permitted`). Por eso esta revisión no acredita una nueva prueba HTTP/broker. Los tests de flujo usan la lógica real del MDB y una cola simulada.

**Huecos del alcance de demo, documentados:**
- El stock sólo se comprueba: no se reserva ni se descuenta. Dos compras pueden aceptar las mismas unidades. `CONFIRMADO` es una confirmación de demostración por ADMIN; no verifica reserva vigente ni pago como plantea el diseño objetivo. No representa una venta comercial completa.
- `REINTENTAR_ENVIO` existe en State, pero no hay endpoint ni tarea que vuelva a publicar un pedido en `ERROR_ENVIO`. El error de creación no entrega al cliente el id guardado. Recuperación automática requiere implementar el ADR-007.
- Un mensaje persistente puede sobrevivir al reinicio, pero su pedido en memoria no: el MDB rechazará el id desconocido y el broker podrá terminar enviándolo a su cola de errores.
- SEC-07 y la conexión del front al JWT siguen pendientes. Las cuentas y la clave del script son de demo; el contador de intentos sólo vive en memoria y no tiene limpieza global de correos desconocidos.

**Commits:** se intentó crear un commit nuevo, pero `.git` está montado como solo lectura: `Unable to create .git/index.lock: Read-only file system`. No se crearon commits, no se reescribió el historial, no se hizo push ni se cambió de rama. Los cambios quedan listos para revisar y commitear desde un entorno con escritura en `.git`.

Los comandos para crear cuatro commits pequeños, con rutas explícitas, están en `.entrega2/COMMITS_REVISION.md`.

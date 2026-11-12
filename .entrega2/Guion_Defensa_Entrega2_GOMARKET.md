# Guion de defensa · 2.ª Entrega Parcial · GOMARKET (Grupo 6)

Entrega del 13/11 · "El núcleo funcionando". Duración sugerida: 10 a 12 minutos de exposición y demo, más preguntas. Todos tienen que poder explicar todo el código, aunque cada uno presente una parte.

**Frase para arrancar:** "GOMARKET es un e-commerce hecho en Jakarta EE. En esta entrega mostramos que el núcleo ya funciona: un cliente se loguea, hace un pedido, el pedido viaja por una cola, un administrador lo confirma, y el sistema no deja hacer cosas imposibles ni entrar a lo que no corresponde."

## Antes de empezar (checklist de demo)

1. Terminal 1: `./scripts/levantar-wildfly.sh` y esperar a que diga *started*.
2. Terminal 2: tener a mano `./scripts/probar-entrega2.sh` y los comandos curl de `RESUMEN.md`.
3. Tener abierto el código: `EstadosPedido.java`, `JwtService.java`, `AutorizadorDeAccesoTest.java` y `ProcesarPedidoMDB.java`.
4. Si el servidor falla: mostrar `./mvnw test` (69 tests en verde) y explicar que la revisión previa informó un recorrido exitoso. En la revisión actual el sandbox no permitió abrir sockets para repetirlo.

---

## Punto 1 · Componentes en capas y flujo de punta a punta

**Qué decir:**
- "Cada componente (catálogo, pedido, seguridad) tiene tres capas: `controller` (recibe HTTP), `service` (las reglas del negocio) y `repository` (donde se guardan los datos). El controller no tiene lógica y el service no sabe nada de HTTP."
- "Un componente no es una pantalla: es una responsabilidad del negocio con su interfaz y sus capas." (Es lo que nos marcó el profe en el TP Inicial.)
- "Mejoramos algo: antes `PedidoService` usaba directamente la clase que guarda en memoria y la API de JMS. Ahora depende de dos interfaces, `PedidoRepository` y `PublicadorPedidos`. Si mañana usamos una base de datos, el service no cambia."

**Demo (1 minuto):** correr `./scripts/probar-entrega2.sh` y señalar la parte 2: *pedido creado → el MDB lo pasó a PROCESADO → admin confirma*.

**Preguntas probables**
- *¿Qué es una capa?* Un grupo de responsabilidades del mismo tipo. Presentación, negocio y datos.
- *¿Por qué una interfaz para el repositorio?* Es el principio D de SOLID: dependemos de una abstracción y no de un detalle. Además nos permitió probar todo el flujo con tests sin base de datos ni servidor.
- *¿Por qué los datos están en memoria?* Es una demo; se pierden al reiniciar. Lo siguiente es una base de datos y el outbox del ADR-007.

## Punto 2 · Patrones: State y el 409

**Qué decir:**
- "El pedido pasa por estados: PENDIENTE, PROCESADO, CONFIRMADO, CANCELADO, ERROR_ENVIO, etc. Antes el estado era un texto y cualquier parte del código podía poner cualquier valor."
- "Con **State**, cada estado es un objeto que sabe qué eventos acepta. `PENDIENTE` acepta 'mensaje procesado' y 'cancelar'; `CONFIRMADO` no acepta 'cancelar'."
- "Si el evento no es válido, el estado lanza `TransicionInvalidaException`, y un mapper la convierte en **HTTP 409**. El pedido queda como estaba."

**Demo (1 minuto):**
```bash
curl -s -X POST $B/pedidos/$ID/confirmar -H "Authorization: Bearer $AT"   # 200 CONFIRMADO
curl -i -X POST $B/pedidos/$ID/cancelar  -H "Authorization: Bearer $CT"   # 409
```
Mostrar el mensaje: *"No se puede aplicar CANCELAR a un pedido en estado CONFIRMADO."*

**Preguntas probables**
- *¿Por qué State y no un `if` o un enum?* Con `if` las reglas se desparraman por el servicio y el MDB. Un enum con tabla serviría, pero State pone el comportamiento dentro de cada estado y es lo que pide la clase; agregar un estado no toca los demás.
- *¿Por qué 409 y no 400?* El 400 es "tu pedido está mal escrito". Acá la solicitud es válida, pero **choca con el estado actual** del recurso. Eso es justo un conflicto: 409.
- *¿Y si dos personas cambian el pedido a la vez?* El repositorio usa `computeIfPresent`, que es atómico: valida la transición y reemplaza el pedido en un solo paso. Si lanza, el mapa no cambia.
- *¿Qué otros patrones usaron?* Strategy (el orden del catálogo en el front), Adapter (el catálogo remoto) y, en el backend, puertos y adaptadores (`PublicadorPedidos` / `PublicadorPedidosJms`).
- *¿Pedido es inmutable?* No completamente: conserva setters para JSON-B e ítems mutables. `transicionar` devuelve un pedido nuevo sin modificar el estado del anterior; las rutas usan esos eventos para cambiarlo.

## Punto 3 · Seguridad: JWT y roles

**Qué decir:**
- "Cuando hacemos login, el servidor verifica la clave y devuelve un **JWT**. Un JWT son tres partes separadas por puntos: cabecera, datos (email, rol, cuándo vence) y firma."
- "La firma se calcula con una clave secreta que solo tiene el servidor (HMAC-SHA256). Si alguien cambia el rol dentro del token, la firma deja de coincidir y lo rechazamos. En el front el rol estaba en localStorage y cualquiera podía ponerse ADMIN (SEC-01). Ahora no."
- "Cada endpoint declara su rol con `@RolesAllowed`. Un filtro lee eso: **sin token → 401**, **token válido pero rol que no alcanza → 403**."
- "Las claves se guardan con hash PBKDF2 y sal, no en claro. Después de 5 intentos fallidos en 15 minutos se bloquea el ingreso (429). El registro siempre crea CLIENTE: si mandan `rol: ADMIN`, se ignora. Y cada cliente solo ve sus pedidos."

**Demo (2 minutos):**
```bash
curl -i -X POST $B/pedidos -H 'Content-Type: application/json' -d '{"items":[{"idProducto":1,"cantidad":1}]}'   # 401
curl -i $B/auth/usuarios -H "Authorization: Bearer $CT"    # 403 (cliente)
curl -i $B/auth/usuarios -H "Authorization: Bearer $AT"    # 200 (admin), sin hashes
```
Opcional: decodificar el payload localmente y mostrar `sub`, `rol`, `exp`.

**Preguntas probables**
- *¿Qué diferencia hay entre 401 y 403?* 401: no sabemos quién sos (falta el token o es inválido). 403: sabemos quién sos pero no tenés permiso.
- *¿Por qué JWT y no sesiones?* El servidor no guarda nada por usuario (stateless): la información viaja firmada en el token. Escala mejor y encaja con una API REST.
- *¿Se puede falsificar un token?* No sin la clave secreta. Además verificamos que el algoritmo sea exactamente HS256 (rechazamos `alg: none`), el emisor y la fecha de vencimiento, y comparamos la firma en tiempo constante.
- *¿El JWT está cifrado?* No, está **firmado**, no cifrado: cualquiera puede leerlo. Por eso no ponemos datos sensibles adentro (ni claves).
- *¿Qué pasa si roban un token?* Sirve hasta que vence (30 min). No hicimos revocación ni refresh token todavía; está anotado como pendiente.
- *¿Por qué PBKDF2 y no Argon2?* Argon2id es la primera opción de OWASP, pero no viene en la JDK. PBKDF2 sí, y OWASP lo acepta con 600.000 iteraciones y sal por usuario.
- *¿Por qué el error de login es siempre el mismo?* Para no revelar si un correo existe (SEC-02). Incluso hacemos un hash de relleno cuando el correo no existe, para que tarde parecido.
- *¿De dónde sale el dueño del pedido?* Del token, nunca del cuerpo de la solicitud. Así nadie puede crear pedidos a nombre de otro.
- *¿De dónde sale la clave de firma?* De `GOMARKET_JWT_SECRET` o la propiedad `gomarket.jwt.secret`, con al menos 32 bytes. Si falta se genera al azar. El script tiene una clave pública sólo de demo; hay que reemplazarla para un despliegue real.
- *¿Y el front?* Todavía usa su sesión local. Conectarlo al login del servidor es lo próximo.

## Punto 4 · Mensajería

**Qué decir:**
- "Al crear un pedido, la API responde enseguida **202 (aceptado)** y deja un mensaje en una cola JMS. Después, un consumidor, el **MDB** `ProcesarPedidoMDB`, lo toma y pasa el pedido a PROCESADO. Es asíncrono: el cliente no espera."
- "Usamos una cola punto a punto, porque cada pedido lo tiene que procesar un solo consumidor."
- "JMS puede entregar un mensaje dos veces. Como el estado lo maneja State, procesarlo de nuevo no rompe nada. Si el cliente canceló antes de que llegue el mensaje, lo descartamos y dejamos un aviso en el log, en vez de reintentar para siempre."

**Demo:** crear un pedido y hacer GET un segundo después: de `PENDIENTE` a `PROCESADO` sin que nadie lo toque. También se ve en el log de WildFly: *"Pedido … procesado por la cola."*

**Preguntas probables**
- *¿Por qué mensajería y no llamar directo?* Desacopla: la API no depende de que el procesamiento esté listo ni rápido, y si el consumidor se cae el mensaje espera en la cola (es persistente).
- *¿Y si falla el envío?* Si sigue pendiente, pasa a `ERROR_ENVIO`. State tiene `REINTENTAR_ENVIO`, pero todavía no hay una ruta o tarea que vuelva a publicarlo. Si el MDB ya lo procesó, conserva `PROCESADO`; el error de envío se propaga sin un 409 interno engañoso.
- *¿Pedido y mensaje son atómicos?* Todavía no: puede guardarse el pedido y fallar el envío. La solución es el patrón outbox/inbox (ADR-007), que es diseño y no está implementado.
- *¿Cómo lo verificaron?* Contra un WildFly real con `standalone-full.xml`, que trae el broker Artemis. En los tests unitarios la cola se reemplaza por una lista, y se prueba la misma lógica del MDB.

## Punto 5 · Tests unitarios

**Qué decir:** "Hay **69 tests de Java** y 90 de JavaScript, todos en verde. Los de Java corren con `./mvnw test`, sin servidor."

| Qué | Qué prueba |
|---|---|
| `PedidoStateTest` (11) | Cada transición válida e inválida, estados finales, el 409 y que el pedido conserve su dueño. |
| `FlujoPedidoTest` (9) | El recorrido completo, pedido ajeno (403), inexistente (404), cola caída, mensajes repetidos. |
| `JwtServiceTest` (9) | Token correcto, rol alterado, otra clave, vencido, `alg=none`, basura. |
| `AuthServiceTest` (7) | Login, bloqueo a los 5 fallos y desbloqueo a los 15 min, registro siempre CLIENTE. |
| `AutorizadorDeAccesoTest` (9) | 401, 403, anotaciones de clase y método, endpoints públicos. |
| `PasswordHasherTest` (3) | Hash con sal distinta; verificación. |
| `AuthResourceTest` (2) | Cuerpos ausentes de login/registro: error 400. |
| `PedidoServiceTest` (5) | Cálculo, stock, duplicados, vacío y carrera de entrega JMS con error del publicador. |
| Catálogo y cuerpo inválido (14) | Búsquedas, filtros y errores de JSON. |

**Preguntas probables**
- *¿Por qué no usaron Mockito?* No hace falta: gracias a las interfaces armamos dobles simples a mano (una cola en memoria y un catálogo falso).
- *¿Qué NO prueban?* El HTTP real ni el broker. Eso lo cubre el script `probar-entrega2.sh` contra WildFly.
- *¿Cómo probaron el bloqueo de 15 minutos sin esperar 15 minutos?* Inyectamos un reloj (`Clock`) que adelantamos en el test.
- *¿Un test que pasa garantiza que es seguro?* No. Son pruebas de ejemplos; las de seguridad prueban los casos de abuso que listó la clase 11, pero no certifican todo.

## Punto 6 · Jira

**Qué decir:** "Dejamos una lista de tarjetas sugeridas con sus estados en JIRA.md. El equipo debe cargarlas al tablero; no lo actualizamos remotamente." *(Lista de tarjetas en `.entrega2/JIRA.md`.)*

## Punto 7 · Feedback del diseño incorporado

**Qué decir:**
- "De la devolución del TP Inicial: **componente ≠ pantalla**. Cada componente tiene responsabilidad, interfaz y tres capas."
- "De la clase 12: aplicamos **State** al pedido y arreglamos la violación de SOLID-D que habíamos marcado (el service dependía de la clase de memoria)."
- "De la clase 11: pasamos al servidor lo que estaba en el navegador: roles (SEC-01), hash de claves (SEC-09), límite de intentos (SEC-03), registro como CLIENTE (SEC-04) y datos mínimos de usuario (SEC-10). Corregimos SEC-06: los nombres y correos se escapan al generar HTML en el menú y el padrón. Sigue pendiente SEC-07 (timeout del cliente HTTP)."
- "Cada decisión quedó escrita en un ADR (carpeta `docs/adr`): 001 State, 008 JWT y roles, 009 REST y no SOAP, 010 interfaces."
- Ser honestos: no encontramos otro feedback textual del profe sobre el diseño; implementamos lo que pide la consigna, el informe de diseño y las clases.

**Pregunta probable**
- *¿Qué dice su informe de diseño que no hicieron?* El adaptador SOAP al inventario legado: lo reemplazamos por la decisión REST (ver abajo). Y el OpenAPI de diseño todavía dice "sin autenticación": falta actualizarlo.

---

## ¿Por qué REST y no SOAP? (pregunta casi segura)

**Respuesta corta:** "Porque GOMARKET es un e-commerce con un front web en JavaScript. REST con JSON es lo natural ahí, y SOAP nos agregaría XML y herramientas que no necesitamos."

**Respuesta completa:**
1. **El que consume la API es el navegador.** JavaScript lee JSON directamente; con SOAP habría que armar y leer XML con un sobre (envelope).
2. **REST usa HTTP tal cual:** métodos (GET/POST), códigos de estado (401, 403, 409, 429), cache y se prueba con `curl`. En SOAP casi todo es un POST y los errores van en un `Fault` dentro de la respuesta. Nuestros errores usan Problem Details (RFC 9457), que son el equivalente.
3. **Menos peso:** SOAP necesita un WSDL, generar clientes y validar XML (en el diseño anterior hasta teníamos que protegernos de ataques XML). Nada de eso aporta a este proyecto.
4. **Para lo asíncrono usamos JMS**, no SOAP.
5. **Cuándo sí usaríamos SOAP:** si un sistema externo (un ERP viejo) solo hablara SOAP. Entonces lo aislaríamos en un adaptador, sin contaminar el resto. Hoy no existe ese sistema, así que no se implementó.
6. En el código no hay JAX-WS, ni WSDL, ni clientes SOAP. Los archivos de `entrega-diseno/contratos` son de la entrega anterior y no se usan.

**Contra-pregunta posible:** *"¿No era un requisito del diseño?"* — "En el diseño planteamos un adaptador SOAP como idea, pero lo revisamos: como no hay un inventario real que lo exija, elegimos REST (ADR-009). Es una decisión consciente."

---

## Preguntas sueltas que pueden aparecer

- **¿Qué es un MDB?** Un Message-Driven Bean: un componente que el servidor activa cuando llega un mensaje a una cola.
- **¿Qué es CDI?** Inyección de dependencias del contenedor: pedimos `@Inject` y el servidor nos da el objeto. Los servicios de seguridad son `@ApplicationScoped` (uno solo para toda la aplicación), por eso la clave JWT es la misma para todos los pedidos.
- **¿Qué es Problem Details?** Un formato estándar para errores en JSON: `type`, `title`, `status`, `detail`, `instance`.
- **¿Qué se llevarían como próximo paso?** Conectar el front al login, base de datos, outbox/inbox, refresh token y actualizar el OpenAPI.
- **¿Qué fue lo más difícil?** Que el servidor real no encontraba nuestros servicios de seguridad: con `@Singleton` CDI no los descubría en modo `annotated`. Lo detectamos al probar en WildFly y no en los tests, y por eso insistimos en probar de punta a punta.

## Cierre (20 segundos)

"En resumen: componentes en capas desplegados, un flujo de pedido completo con mensajería, State que responde 409, JWT con roles, 69 tests en verde, y las decisiones documentadas. Lo que falta está en Jira como pendiente."


## Aclaraciones de la revisión del 12/11

- **¿Confirmar equivale a cobrar o reservar stock?** No. Es una transición de demostración por ADMIN. Todavía comprobamos disponibilidad sin reservar ni descontar unidades. La reserva vigente y el pago del diseño son trabajo pendiente.
- **¿Qué corrigieron en esta revisión?** La clave JWT corta en el arranque real, errores de registro que terminaban en 500, `DenyAll` de clase, la carrera entre publicación y consumo JMS, y nombres que se interpretaban como HTML. Hay 69 tests Java y 90 JS en verde.
- **¿Se volvió a probar el servidor?** Se intentó en una carpeta temporal, pero este sandbox impide crear sockets. No afirmamos una nueva ejecución HTTP/JMS exitosa; conservamos la referencia de la revisión previa y los tests de flujo con cola simulada.
- **¿Qué pasa al reiniciar?** Se pierden los pedidos en memoria. Un mensaje persistente puede quedar sin pedido y terminar en la cola de errores. Falta persistencia y outbox/inbox.
- **¿Se commiteó esta revisión?** No: `.git` está en solo lectura en este entorno. Los cambios quedan preparados en `entrega-2`; no se hizo push ni se tocó `main`.

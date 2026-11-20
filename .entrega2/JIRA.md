# Tarjetas sugeridas para Jira · 2.ª entrega (13/11)

Las armamos nosotros a mano (no se cargaron solas). Estados: **Hecho** = ya está en el código y con tests; **En revisión** = hecho pero falta que otra persona lo pruebe; **Pendiente** = todavía no.

## Épica: Núcleo funcionando

| # | Título | Descripción corta | Estado |
|---|--------|-------------------|--------|
| 1 | Pasar el State del pedido al código | El pedido ahora tiene estados (PENDIENTE, PROCESADO, CONFIRMADO, etc.) y cada estado sabe qué puede pasar después. Antes cualquier texto valía como estado. | Hecho |
| 2 | Devolver 409 si la transición no se puede | Si pedís algo imposible (por ejemplo cancelar un pedido ya confirmado), la API contesta 409 con un mensaje claro y el pedido no cambia. | Hecho |
| 3 | Agregar el estado CANCELADO | El cliente puede cancelar su pedido mientras no esté confirmado. Cancelado es un estado final. | Hecho |
| 4 | Login con JWT | `POST /api/auth/login` verifica la clave en el servidor y devuelve un token firmado que dura 30 minutos. | Hecho |
| 5 | Roles CLIENTE y ADMIN en el servidor | Cada endpoint dice qué rol necesita. Sin token da 401, con el rol equivocado da 403. El rol ya no se puede editar desde el navegador (SEC-01). | Hecho |
| 6 | Guardar las claves con hash y sal | Las claves ya no están en el navegador: el servidor guarda un hash PBKDF2 con sal (SEC-09). | Hecho |
| 7 | Bloquear tras 5 intentos fallidos | Con 5 claves malas en 15 minutos, la cuenta queda bloqueada aunque después pongan la clave correcta (SEC-03). Responde 429. | Hecho |
| 8 | Registro siempre como CLIENTE | Si alguien manda `rol: ADMIN` al registrarse, se ignora (SEC-04). | Hecho |
| 9 | Cada cliente ve solo sus pedidos | El dueño del pedido sale del token. Otro cliente recibe 403; el ADMIN ve todos. | Hecho |
| 10 | Verificar que la mensajería JMS anda | Al crear un pedido se manda un mensaje a la cola y el MDB lo pasa a PROCESADO. Probado en WildFly real. | Hecho |
| 11 | Mensajes repetidos o de pedidos cancelados | Si la cola entrega dos veces el mismo mensaje no pasa nada; si el pedido se canceló antes, el mensaje se descarta con un aviso. | Hecho |
| 12 | Separar el código con interfaces (SOLID) | `PedidoService` y el MDB ahora dependen de interfaces (`PedidoRepository`, `PublicadorPedidos`) y no de la memoria ni de JMS. | Hecho |
| 13 | Tests unitarios de seguridad | 25 tests de JWT, hash de claves, login, bloqueo y roles. | Hecho |
| 14 | Tests del State y del flujo de pedido | 20 tests: transiciones válidas e inválidas, y el recorrido completo login → pedido → cola → confirmar. | Hecho |
| 15 | Script para probar todo con curl | `scripts/probar-entrega2.sh` recorre login, roles, pedido, mensajería y el 409. | Hecho |
| 16 | Sacar SOAP del proyecto | No hay SOAP en el código. Todo es REST/JSON. Quedó escrito el motivo en el ADR-009. | Hecho |
| 17 | Escribir los ADR 008, 009 y 010 | Explican por qué JWT con roles, por qué REST y no SOAP, y por qué usamos interfaces. | Hecho |

## Pendientes (para después de la entrega)

| # | Título | Descripción corta | Estado |
|---|--------|-------------------|--------|
| 18 | Conectar el front al login del servidor | Hoy la página usa una sesión local en el navegador. Hay que cambiarla para que use `/api/auth/login` y mande el token. | Pendiente |
| 19 | Pasar usuarios y pedidos a una base de datos | Ahora están en memoria y se pierden al reiniciar. | Pendiente |
| 20 | Outbox/inbox para no perder mensajes | Está en el ADR-007 como diseño. Falta implementarlo. | Pendiente |
| 21 | Timeout de 5 segundos en el cliente HTTP del front (SEC-07) | El front espera sin límite si la API no responde. | Pendiente |
| 22 | Mostrar el nombre de usuario como texto (SEC-06) | Se escapan nombres y correos en el menú y el padrón; dos pruebas evitan que vuelva el problema. | Hecho |
| 23 | Refresh token y revocar tokens al cerrar sesión | Un token robado hoy sirve hasta que vence. | Pendiente |
| 24 | Actualizar el OpenAPI de la entrega de diseño | Falta sumar login, 401/403/409/429 y el campo `cliente`. | Pendiente |
| 25 | Revisión cruzada del código de seguridad | Que otra persona del grupo pruebe los casos SEC-01, SEC-03 y SEC-09. | En revisión |

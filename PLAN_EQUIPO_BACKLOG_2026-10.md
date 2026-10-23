# Plan de equipo y backlog — GOMARKET

Equipo: **Rodrigo, Nicolás, Tomás, Iván y Fol**  
Última revisión: **09/10/2026**  
Próximo hito del calendario de la comisión: **entrega del diseño — 06/11/2026**

## Calendario vigente de la comisión (captura compartida el 09/10)

| Fecha | Actividad / hito |
|---|---|
| 09/10 | Clase 10: arquitecturas de integración — REST |
| 16/10 | Clase 11: aplicación integrada — seguridad |
| 23/10 | Clase 12: patrones de diseño aplicados al TPO |
| 30/10 | Clase 13: IA integrada al TPO |
| **06/11** | **Entrega del diseño del TP obligatorio** |
| **13/11** | **2.ª entrega parcial** |
| **20/11** | **3.er checkpoint del TPO** |
| 27/11 | Prueba del TP obligatorio (testing) |
| 04/12 | Recuperatorio / final adelantado |
| 11/12 | Final regular |

La captura también ubica Servicios Web el 02/10. Las fechas **19/10,
09/11 y 30/11** de los planes anteriores corresponden a otro cronograma o a
objetivos internos: **no deben presentarse como fechas oficiales de esta
comisión**. Confirmar con el docente qué funcionalidades y documentos pide en
cada uno de los hitos del 06, 13 y 20/11; la captura fija fechas y nombres,
pero no detalla criterios de entrega.

## Roles del equipo

Los roles indican quién conduce cada área, pero todos programan, documentan y
revisan trabajo ajeno. Cada tarjeta debe tener responsable y revisor distintos.

| Integrante | Rol principal | Responsabilidades |
|---|---|---|
| **Rodrigo** | Líder y coordinador | Priorizar el backlog, destrabar decisiones, cuidar la arquitectura, integrar las partes y conducir las dailies y la demo |
| **Iván** | Backend | Maven/WildFly, API, servicios, productor y consumidor JMS, configuración y despliegue reproducible |
| **Tomás** | Frontend | Interfaz responsive, Figma, estados de carga/error y flujo de compra |
| **Fol** | Datos | Modelo y persistencia de pedidos, contratos de datos, trazabilidad y datos de prueba |
| **Nicolás** | QA | Plan de pruebas, casos exitosos y fallidos, evidencias, regresión y revisión de calidad |

Esta asignación reproduce el acuerdo del equipo del 09/10. Son **áreas principales**, no
permisos exclusivos: todos pueden colaborar fuera de su área y cada entrega requiere
revisión cruzada. No confundir estos roles de trabajo con `CLIENTE` / `ADMIN`, que son
roles de autorización de la aplicación.

### Acuerdos de trabajo

- Rodrigo decide la prioridad cuando haya dudas, pero no concentra todas las tareas.
- Máximo una tarjeta En curso por persona.
- Ninguna tarjeta pasa a Hecho sin evidencia y revisión de otra persona.
- Cada integrante debe tener commits técnicos propios antes del checkpoint.
- Si una tarjeta permanece bloqueada más de un día, se avisa al equipo.

## Sprint interno — 02/10 al 19/10

Objetivo: demostrar un **primer proceso asincrónico real** del pedido mediante
JMS, acompañado por arquitectura SOA, documentación funcional, manual y
prototipo navegable. **El 19/10 es una meta interna de preparación, no una
entrega oficial según el calendario vigente.**

### Backlog priorizado

| Prioridad | ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|---|
| P0 | `[SPR3-01] Auditar estado real y cerrar base del sprint` | Rodrigo | Nicolás | 05/10 | Ramas pendientes integradas o registradas, build verde y faltantes anteriores visibles |
| P0 | `[SPR3-02] Definir arquitectura SOA y flujo asincrónico` | Rodrigo | Tomás | 06/10 | Diagrama con productor, cola, consumidor, base y frontera; se justifica la asincronía |
| P0 | `[SPR3-03] Configurar Artemis y ProcesarPedidoQueue` | Iván | Nicolás | 07/10 | Cola configurada en WildFly/Artemis con pasos reproducibles y prueba de conexión |
| P0 | `[SPR3-04] Definir contrato del mensaje Pedido` | Fol | Iván | 07/10 | Payload, campos obligatorios, identificador, versión y política de errores documentados |
| P0 | `[SPR3-05] Implementar productor de pedidos` | Iván | Nicolás | 10/10 | Una operación publica el mensaje sin acoplarse al consumidor y devuelve respuesta comprobable |
| P0 | `[SPR3-06] Implementar consumidor MDB` | Iván | Nicolás | 12/10 | Un MDB consume el mensaje, registra el resultado y demuestra el recorrido completo |
| P0 | `[SPR3-07] Integrar flujo mínimo desde la interfaz` | Tomás | Nicolás | 14/10 | La UI inicia el proceso y muestra envío, aceptación y error sin bloquearse |
| P1 | `[SPR3-08] Crear prototipo navegable en Figma` | Tomás | Fol | 11/10 | Recorrido catálogo → carrito → confirmación accesible para equipo y docente |
| P1 | `[SPR3-09] Escribir historias y casos de uso` | Rodrigo | Tomás | 11/10 | Actores, precondiciones, flujo principal, alternativas y criterios de aceptación |
| P1 | `[SPR3-10] Crear manual de usuario` | Tomás | Nicolás | 14/10 | Guía breve de cliente y administrador con capturas o espacios claramente marcados |
| P1 | `[SPR3-11] Preparar pruebas y evidencia de mensajería` | Nicolás | Iván | 16/10 | Casos exitoso y fallido repetibles, logs/capturas y pasos documentados |
| P1 | `[SPR3-12] Consolidar documento y trazabilidad` | Rodrigo | Fol | 16/10 | Documento enlaza arquitectura, proceso, código, tablero, Figma y manual |
| P1 | `[SPR3-13] Ensayar demo y defensa cruzada` | Los cinco | Rodrigo | 17/10 | Demo cronometrada; cada uno explica su parte y una ajena; hay contingencia |
| P1 | `[SPR3-14] Revisión interna del sprint` | Los cinco | Los cinco | 19/10 | Build limpio, tablero actualizado, enlaces accesibles y brechas para los hitos de noviembre registradas |
| P2 | `[SPR3-15] Agregar reintento o cola de errores` | Iván | Nicolás | 17/10 | Un fallo queda visible y puede reintentarse o aislarse sin perder el mensaje |

**Estado al 09/10:** las fechas 05–07/10 son objetivos del plan original ya
vencidos, no evidencia de finalización. Confirmar avance real y bloqueos en la
daily antes de mover tarjetas. No marcar ninguna tarea como hecha sólo por este
ajuste de responsables.

**Avance técnico comprobado el 09/10:** se desplegó el WAR en WildFly
`standalone-full.xml`; `POST /api/pedidos` devolvió `202`, el GET posterior
mostró `PROCESADO` por el MDB y una cantidad superior al stock devolvió `400`.
Hay código para `SPR3-03`, `SPR3-05`, `SPR3-06` y `SPR3-07`, pero **no se marcan
como Hecho** hasta revisión cruzada y evidencia del equipo. Ver
[`docs/ENTREGA_AVANCE_2026-10-09.pdf`](docs/ENTREGA_AVANCE_2026-10-09.pdf).

### Trazabilidad con las últimas clases

| Fuente | Exigencia aplicable | Tarjetas/evidencia |
|---|---|---|
| Clase 8, Unidad V (Mensajería) | Elegir un proceso real y preparar **productor → broker → consumidor** funcionando en vivo, no sólo diagramado. La diapositiva cita 19/10, pero el calendario vigente ubica el 3.er checkpoint el **20/11** | `SPR3-02` a `SPR3-07`, `SPR3-11`; demo y logs; confirmar alcance con docente |
| Clase 8, Unidad V | Justificar **cola** para un único procesador del pedido; definir formato y contenido del mensaje y comportamiento si el consumidor cae | `SPR3-04`, `SPR3-06`, `SPR3-11`; contrato y prueba de caída/recuperación |
| Clase 9, Unidad VI (Servicios Web) | Diseñar SOAP/WSDL, operación, entrada, salida, Fault, frontera síncrona/asíncrona y timeout | [`clase09/Actividad_Clase09_ServiciosWeb.pdf`](clase09/Actividad_Clase09_ServiciosWeb.pdf); fecha y alcance de implementación del mock SOAP por confirmar |
| Clase 8 | Mantener el tablero actualizado: el docente puede pedirlo en cualquier clase | Trello y este plan deben reflejar el mismo estado real; sincronización remota **pendiente de verificar** |

El tópico `PedidoConfirmadoTopic` y la integración SOAP/REST completa estaban
previstos en una etapa posterior en el plan anterior; **no se asignan a una
fecha oficial nueva sin confirmar el alcance con el docente**. El documento de
Clase 9 es un **diseño hipotético del ERP legado**, no una integración ya
implementada en GOMARKET.

## Esta semana — hasta la daily del 09/10

Mover a **Esta semana** sólo `SPR3-01`, `SPR3-02`, `SPR3-03`, `SPR3-04`,
`SPR3-05` y `SPR3-08`. El resto queda en Backlog.

## Próxima daily — viernes 09/10/2026

Duración: **15 minutos**. Facilita Rodrigo. Cada persona dispone de dos minutos
y responde: qué terminó, qué hará ahora y qué la bloquea.

| Orden | Integrante | Compromiso a revisar |
|---|---|---|
| 1 | Rodrigo | Build base estable, tablero actualizado y arquitectura SOA revisable |
| 2 | Iván | Artemis, `ProcesarPedidoQueue` y avance del productor con evidencia |
| 3 | Tomás | Primer flujo navegable de Figma y estados previstos para la interfaz |
| 4 | Fol | Contrato de datos `Pedido` y esquema de persistencia propuesto |
| 5 | Nicolás | Plan de QA, prueba de conexión/caída y evidencia reproducible |

### Agenda

1. **0–2 min:** Rodrigo recuerda la meta interna del 19/10 y los hitos oficiales del 06, 13 y 20/11.
2. **2–12 min:** ronda de los cinco, sin resolver problemas durante la ronda.
3. **12–14 min:** identificar bloqueos y asignar quién los destraba.
4. **14–15 min:** confirmar responsable, revisor y próxima acción.

## Definition of Done

- [ ] Cumple el criterio de aceptación.
- [ ] Compila y puede demostrarse desde un entorno limpio.
- [ ] Incluye prueba o procedimiento reproducible.
- [ ] Tiene commit/PR o documento enlazado.
- [ ] Fue revisada por una persona distinta.
- [ ] La documentación y el tablero reflejan el estado real.
- [ ] Responsable y revisor pueden explicarla.

## Después del sprint interno

- Integración REST con pasarela moderna.
- Integración SOAP con facturador legado y WSDL.
- Tópico `PedidoConfirmadoTopic` con notificación y auditoría.
- Checkout transaccional con rollback o compensación demostrable.
- Documento final, auditoría de requisitos y defensa cruzada.

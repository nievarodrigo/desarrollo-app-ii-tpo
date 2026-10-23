# Plan Trello simple — TPO E-commerce

> **Plan vigente desde el 02/10/2026:** ver
> [`PLAN_EQUIPO_BACKLOG_2026-10.md`](PLAN_EQUIPO_BACKLOG_2026-10.md).
> Este archivo queda como historial del checkpoint anterior.

Equipo: **Rodrigo, Nicolás, Tomás, Iván y Fol**  
Fuente: consigna principal + anexo oficial  
Última revisión: **28/08/2026**

## Cómo usamos el tablero

El tablero muestra sólo lo que ayuda a decidir qué hacer ahora. No se cargan
decenas de tareas futuras por adelantado.

Listas: **Decisiones · Esta semana · En curso · Revisión cruzada · Hecho · Después**.

- En **Esta semana** sólo entran las tarjetas del próximo hito.
- Cada integrante puede tener como máximo **una tarjeta en curso**.
- **Después** contiene hitos grandes, no subtareas. Se desglosan recién al
  comenzar la entrega correspondiente.
- Responsable y revisor pueden definirse al tomar la tarjeta; no hace falta
  repartir todo el cuatrimestre hoy.
- Una tarjeta se considera terminada con evidencia mínima: enlace a commit o
  documento, prueba reproducible y revisión de otra persona.

## Estado real al 28/08

Ya está hecho: proyecto Maven WAR, catálogo stateless en capas, API JAX-RS,
frontend responsive conectado al backend, despliegue WildFly comprobado, landing
en Vercel y 12 pruebas unitarias. La persistencia todavía es en memoria.

Para el checkpoint del **31/08** la consigna no exige JPA ni implementar todos
los componentes: exige identificar al menos seis, documentar sus interfaces,
mostrar uno completo en capas dentro de un contenedor real, justificar el stack
y el carácter stateful/stateless, y entregar un documento breve de 2 a 4 páginas.

## ESTA SEMANA — entrega parcial 1 (31/08)

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[AHORA-01] Cerrar decisiones con el docente` | Rodrigo | Nicolás | 28/08 | Quedan registradas la aprobación del e-commerce, la excepción del grupo de 5, la fecha final y la aceptación de Trello; si falta respuesta, se adjunta la consulta enviada y se marca el bloqueo |
| `[AHORA-02] Ordenar accesos y enlaces de entrega` | A definir | A definir | 29/08 | Repositorio, Trello y landing son accesibles; el docente tiene lectura y los enlaces están juntos en un único lugar |
| `[AHORA-03] Cerrar arquitectura e interfaces` | A definir | A definir | 29/08 | Documento con al menos 6 componentes, responsabilidad, operaciones principales, diagrama de dependencias y justificación del stack Jakarta EE |
| `[AHORA-04] Verificar demo limpia del catálogo` | A definir | A definir | 29/08 | En otra máquina o desde cero se compila el WAR, despliega en WildFly y se demuestra catálogo → service → repository sin errores |
| `[AHORA-05] Preparar documento breve de 2 a 4 páginas` | A definir | A definir | 30/08 | PDF ajustado al formato del checkpoint: arquitectura, componentes, stack, capas y justificación de Catálogo como stateless; se declara el uso de IA |
| `[AHORA-06] Revisar y entregar checkpoint` | Los cinco | Los cinco | 31/08 | Checklist final, tablero actualizado, PDF y enlaces enviados antes de las 13:30; una persona distinta reproduce la demo |

## PRÓXIMA ENTREGA — obligatoria 1 (14/09)

Estas tarjetas quedan en **Después** hasta cerrar el checkpoint del 31/08. Recién
entonces se dividen y asignan.

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[PROX-01] Persistir Catálogo con JPA` | A definir | A definir | 04/09 | Se reemplaza el repositorio en memoria sin cambiar el service y hay datos de prueba reproducibles |
| `[PROX-02] Implementar Carrito stateful` | A definir | A definir | 06/09 | Carrito por capas, estado conversacional y callbacks de ciclo de vida demostrables |
| `[PROX-03] Implementar Usuarios y seguridad` | A definir | A definir | 07/09 | Login y autorización declarativa por rol protegen al menos dos operaciones sensibles |
| `[PROX-04] Implementar Pedido y Checkout` | A definir | A definir | 09/09 | Tercer componente funcional y CheckoutFacade coordinando el flujo sin mezclar responsabilidades |
| `[PROX-05] Cerrar patrones, demo y documento` | Los cinco | Los cinco | 12/09 | Tres componentes desplegados, tres patrones justificados, demo repetible y documento de 5 a 8 páginas |

## DESPUÉS — sólo hitos

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[HITO-03] Preparar checkpoint de mensajería` | A definir | A definir | 19/10 | Primer proceso asincrónico real, diagrama SOA, documentación funcional, manual y prototipo navegable |
| `[HITO-04] Preparar integración completa` | A definir | A definir | 09/11 | Cola, tópico, SOAP, REST y transacción declarativa funcionando en una demo end-to-end |
| `[HITO-FINAL] Preparar entrega final` | Los cinco | Los cinco | 30/11 | Sistema completo, documento de 10 a 15 páginas, historial Git incremental y defensa cruzada |

## Definition of Done corta

- [ ] Funciona y puede demostrarse.
- [ ] Tiene evidencia enlazada (commit, prueba o documento).
- [ ] Otra persona lo revisó.
- [ ] El tablero refleja el estado real.
- [ ] Responsable y revisor pueden explicarlo.

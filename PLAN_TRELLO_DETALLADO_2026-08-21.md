# Plan Trello — TPO E-commerce (reiniciado)

Equipo: **Rodrigo, Nicolás, Tomás, Iván y Fol**  
Fuente principal: `TP_Integrador_DesarrolloAplicacionesII- Viernes.docx`  
Complemento: `Anexo_TP_Integrador_DesarrolloAplicacionesII.docx`

## 0. Bloqueos que deben resolverse primero

1. **Dominio:** e-commerce es un dominio propio y necesita aprobación docente.
2. **Equipo:** la consigna admite grupos de 3 a 4; somos 5. Necesitamos una excepción explícita.
3. **Fecha final:** los documentos mencionan 30/11, 04/12, 11/12 y 21/12. Confirmar cuál rige.
4. **Herramienta:** validar Trello como alternativa a Jira y dar lectura al docente.

Podemos preparar arquitectura y entorno, pero no conviene comprometer todo el desarrollo hasta resolver dominio y cantidad de integrantes.

## 1. Checklist técnico obligatorio

- Mínimo 6 componentes con interfaces explícitas.
- 1 componente stateful y 1 stateless, justificados y con ciclo de vida evidenciado.
- Presentación → negocio → datos en cada componente implementado.
- 3 patrones distintos implementados y justificados.
- Seguridad declarativa en 2 operaciones sensibles.
- 1 integración SOAP con un sistema legado y WSDL.
- 1 integración REST con un partner moderno o API externa.
- 1 proceso asincrónico con cola punto a punto.
- 1 proceso asincrónico con tópico pub/sub.
- 1 flujo crítico con transacciones declarativas.
- Git incremental, responsive, documentación, manual, Figma y demos reales.

## 2. Arquitectura propuesta para el e-commerce

| Componente | Tipo | Responsabilidad |
|---|---|---|
| Usuarios | Stateless | Registro, autenticación, perfiles y roles |
| Catálogo | Stateless | Productos, categorías, precios y búsqueda |
| Inventario | Stateless | Disponibilidad, reserva y descuento de stock |
| Carrito | Stateful | Estado conversacional del carrito del cliente |
| Pedidos/Checkout | Stateless + Facade | Validar, cobrar y confirmar la compra |
| Pagos | Stateless + REST Adapter | Pasarela moderna |
| Facturación Legacy | Stateless + SOAP Adapter | ERP/facturador legado con WSDL |
| Notificaciones | MDB | Consumir eventos y avisar por canales |

**REST:** pasarela moderna. **SOAP:** ERP/facturador legado. **Cola:** `ProcesarPedidoQueue`. **Tópico:** `PedidoConfirmadoTopic`, consumido por notificaciones y auditoría. **Transacción:** reserva de stock + pedido + estado. **Patrones:** DAO/Repository, Facade, Adapter y Strategy como cuarto patrón útil.

## 3. Tablero

Listas: **Información y decisiones · Backlog priorizado · Próxima entrega · En curso · Revisión cruzada · Bloqueado · Hecho**.

Etiquetas: `Bloqueante`, `Arquitectura`, `Backend`, `Frontend`, `Datos`, `Seguridad`, `Integración`, `Mensajería`, `Documentación`, `Testing/DevOps`.

### Reglas

- Cada tarjeta tiene responsable y revisor distinto; nadie revisa su propio trabajo.
- Los cinco deben tener commits técnicos, revisar y documentar.
- Cada PR necesita revisión y cada bloqueo debe registrar causa y próximo intento.
- Registrar por entrega qué IA se usó y para qué.
- En ensayos, cada persona explica una parte que no implementó.

### Definition of Done

- [ ] Criterios de aceptación cumplidos.
- [ ] Capas respetadas y dependencias inyectadas.
- [ ] Pruebas o procedimiento reproducible adjuntos.
- [ ] Revisión cruzada aprobada.
- [ ] Diagramas y documentación actualizados.
- [ ] Commit/PR enlazado y sin secretos.
- [ ] Funciona en otra máquina.
- [ ] Responsable y revisor pueden defenderlo.

## 4. Distribución principal

| Integrante | Liderazgo | Secundario |
|---|---|---|
| Rodrigo | Arquitectura, Pedidos/Checkout, transacciones | Coordinación e integración global |
| Nicolás | Plataforma, Catálogo/Inventario, REST | Maven/WildFly, persistencia y deploy |
| Tomás | Usuarios/Seguridad, SOAP legado | Documentación y revisión de datos |
| Iván | Carrito stateful, broker, cola, testing | Ciclo de vida y demo |
| Fol | Frontend responsive, Figma, Notificaciones | Tópico, manual y UX |

El liderazgo no implica propiedad exclusiva.


# 5. Tarjetas

## INICIO — resolver esta semana

Resolver autorizaciones y preparar el entorno.

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[INI-01] Aprobar dominio e-commerce` | Rodrigo | Nicolás | 24/08 | Respuesta escrita del docente adjunta |
| `[INI-02] Aprobar excepción de grupo de 5` | Rodrigo | Tomás | 24/08 | Confirmación explícita para trabajar los cinco |
| `[INI-03] Confirmar fecha real de entrega final` | Nicolás | Rodrigo | 24/08 | Fecha oficial registrada en Trello y README |
| `[INI-04] Validar Trello y acceso docente` | Tomás | Fol | 24/08 | Trello aceptado y docente con lectura |
| `[INI-05] Crear repositorio, ramas y permisos` | Nicolás | Iván | 24/08 | Repo accesible, main protegida y PR obligatoria |
| `[INI-06] Preparar JDK, Maven, WildFly y base` | Iván | Nicolás | 25/08 | Los cinco ejecutan el mismo entorno |
| `[INI-07] Crear matriz de requisitos y registro IA` | Fol | Tomás | 25/08 | Requisitos trazables y declaración de IA lista |
| `[WEB-01] Crear landing real del e-commerce y desplegar` | Fol | Rodrigo | 27/08 | Landing responsive pública en Vercel, no informativa del TP |

## ENTREGA PARCIAL 1 — 31/08

Arquitectura, 6+ componentes y primer componente en capas desplegado. Cierre interno: **29/08**.

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[E1-01] Documentar 8 componentes e interfaces` | Rodrigo | Fol | 26/08 | Responsabilidad y operaciones por interfaz |
| `[E1-02] Dibujar arquitectura y dependencias` | Fol | Rodrigo | 27/08 | Diagrama legible y coherente |
| `[E1-03] Justificar stack Jakarta EE` | Nicolás | Tomás | 26/08 | ADR de Java, Maven, WildFly, JPA, JAX-RS y Artemis |
| `[E1-04] Crear proyecto Maven WAR desplegable` | Nicolás | Iván | 26/08 | Build y despliegue limpio en WildFly |
| `[E1-05] Implementar ProductoRepository con JPA` | Tomás | Nicolás | 27/08 | Persistencia y datos de prueba funcionando |
| `[E1-06] Implementar CatalogoService y Resource` | Iván | Rodrigo | 28/08 | Listado/detalle atravesando las tres capas |
| `[E1-07] Conectar catálogo responsive al backend` | Fol | Tomás | 28/08 | UI consume datos reales en 3 tamaños |
| `[E1-08] Justificar stateless y redactar 2–4 páginas` | Nicolás | Fol | 29/08 | Documento, evidencia y tablero actualizados |

## ENTREGA OBLIGATORIA 1 — 14/09

Tres componentes, stateful/stateless, ciclo de vida, tres patrones y seguridad. Cierre: **12/09**.

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[E2-01] Completar Catálogo stateless y callbacks` | Nicolás | Rodrigo | 04/09 | Lifecycle y pruebas evidenciados |
| `[E2-02] Implementar Carrito stateful` | Iván | Tomás | 06/09 | Estado conversacional y callbacks reales |
| `[E2-03] Implementar Usuarios y autenticación` | Tomás | Fol | 06/09 | Registro/login por capas |
| `[E2-04] Proteger dos operaciones por rol` | Tomás | Rodrigo | 08/09 | Cliente/Admin y autorización declarativa comprobada |
| `[E2-05] Implementar Pedidos y CheckoutFacade` | Rodrigo | Iván | 08/09 | Orquestación sin mezclar responsabilidades |
| `[E2-06] Integrar login, carrito y checkout` | Fol | Nicolás | 09/09 | Flujo responsive conectado a backend |
| `[E2-07] Aplicar DAO, Facade y Strategy` | Rodrigo | Tomás | 10/09 | Código, problema y alternativa descartada |
| `[E2-08] Pruebas, documento 5–8 páginas y defensa` | Iván | Los cinco | 12/09 | Demo repetible y defensa cruzada |

## ENTREGA PARCIAL 2 — 19/10

Arquitectura SOA, primera mensajería real, documentación funcional, manual y Figma. Cierre: **17/10**.

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[E3-01] Dibujar arquitectura SOA` | Rodrigo | Fol | 03/10 | Internos, externos y canales identificados |
| `[E3-02] Clasificar SOAP, REST, cola y tópico` | Tomás | Rodrigo | 04/10 | Justificación síncrona/asincrónica |
| `[E3-03] Configurar ActiveMQ Artemis y cola` | Iván | Nicolás | 07/10 | Broker y ProcesarPedidoQueue operativos |
| `[E3-04] Implementar primer proceso asincrónico` | Rodrigo | Iván | 10/10 | Productor/consumidor end-to-end |
| `[E3-05] Crear prototipo navegable en Figma` | Fol | Nicolás | 12/10 | Catálogo a confirmación, con acceso docente |
| `[E3-06] Escribir historias y casos de uso` | Tomás | Fol | 12/10 | Actores y criterios de aceptación |
| `[E3-07] Crear manual de usuario` | Nicolás | Tomás | 14/10 | Guía de cliente y administrador |
| `[E3-08] Probar mensajería y preparar evidencia` | Iván | Los cinco | 17/10 | Mensajes y demo comprobados |

## ENTREGA OBLIGATORIA 2 — 09/11

Cola, tópico, SOAP, REST, transacción y demo end-to-end. Cierre: **07/11**.

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[E4-01] Integrar pasarela moderna vía REST` | Nicolás | Rodrigo | 27/10 | Cliente REST detrás de Adapter |
| `[E4-02] Integrar facturador legado vía SOAP` | Tomás | Fol | 29/10 | WSDL y cliente SOAP justificados |
| `[E4-03] Completar cola punto a punto` | Iván | Nicolás | 30/10 | Un pedido procesado por un consumidor |
| `[E4-04] Implementar tópico PedidoConfirmado` | Fol | Iván | 01/11 | Notificación y auditoría reciben el evento |
| `[E4-05] Implementar checkout transaccional` | Rodrigo | Tomás | 02/11 | Rollback o compensación demostrada |
| `[E4-06] Completar Adapter SOAP/REST` | Tomás | Rodrigo | 03/11 | Contratos desacoplados y patrón justificado |
| `[E4-07] Integrar UI, estados y errores` | Fol | Nicolás | 04/11 | Loading, éxito y error responsive |
| `[E4-08] Pruebas, secuencias, documento y defensa` | Iván | Los cinco | 07/11 | Demo cruza componentes, JMS y web services |

## ENTREGA FINAL — FECHA A CONFIRMAR

Sistema completo, documento 10–15 páginas, Git incremental y defensa individual.

| ID y tarjeta | Responsable | Revisor | Vence | Criterio de aceptación |
|---|---|---|---|---|
| `[FIN-01] Auditar checklist transversal` | Rodrigo | Fol | T-14 | Cada requisito enlazado a código, prueba y tarjeta |
| `[FIN-02] Ejecutar regresión y deploy desde cero` | Iván | Nicolás | T-10 | Instalación reproducible y bugs críticos cerrados |
| `[FIN-03] Consolidar documento final 10–15 páginas` | Fol | Tomás | T-8 | Arquitectura, patrones, integración y aprendizajes |
| `[FIN-04] Auditar Git, Trello y declaración IA` | Tomás | Rodrigo | T-8 | Participación de los cinco visible |
| `[FIN-05] Preparar demo y contingencia` | Nicolás | Iván | T-5 | Demo cronometrada y recuperación conocida |
| `[FIN-06] Ensayar defensa cruzada` | Los cinco | Rodrigo | T-3 | Cada uno responde sobre componentes ajenos |
| `[EXTRA-01] Implementar temas normal, claro y oscuro` | Fol | Iván | Opcional | Cambio en tiempo real y responsive |
| `[EXTRA-02] Crear ADR y resiliencia básica` | Rodrigo | Nicolás | Opcional | Decisiones y caída de externo demostradas |

## 6. Rotación para la defensa

- Rodrigo explica Usuarios/Seguridad y SOAP.
- Nicolás explica Carrito stateful y tópico JMS.
- Tomás explica Catálogo/Inventario y transacciones.
- Iván explica Frontend/Figma y REST.
- Fol explica Maven/WildFly, DAO y cola.

## 7. Rutina

- Lunes: 20 minutos de planificación después de clase.
- Jueves: 15 minutos para bloqueos.
- Actualizar Trello al iniciar y terminar cada sesión.
- Ninguna tarjeta más de 3 días en curso sin comentario.
- Congelar funcionalidad 48 horas antes de entregar.

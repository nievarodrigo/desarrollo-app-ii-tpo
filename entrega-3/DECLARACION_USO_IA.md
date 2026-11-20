# Declaración de uso de IA generativa · GOMARKET · Grupo 5

Desarrollo de Aplicaciones II · Comisión Viernes TM · 3.er checkpoint (20/11/2026)
Integrantes: Rodrigo Nieva, Folco Suarez, Iván Karkoszka, Tomás Ruzak, Nicolás Massimino.

Esta declaración cumple la eliminatoria E7 y la regla de la Clase 14: *usar IA sin declararlo en el TPO* está prohibido. Distingue los dos usos que plantea la cátedra: la **IA como herramienta del equipo** (no queda dentro del sistema) y la **IA como componente del producto** (la búsqueda en lenguaje natural).

## 1. IA como herramienta del equipo

| Herramienta | Modelo | Para qué se usó en este checkpoint | Qué hizo el equipo |
|---|---|---|---|
| Claude Code (Anthropic) | Claude Opus 5.5 | Implementación de la resiliencia REST (Idempotency-Key, ETag, Problem Details), de la mensajería (DLQ, consumidor idempotente, reenvío, tópico), del gateway de IA, del pipeline de CI, de los tests y de los scripts de demo; redacción del plan de pruebas, la matriz, el manual, los ADR 011–012 y el material de preparación de la exposición; armado de la presentación | Definió el alcance y las prioridades, decidió que SOAP no aplica, revisó los cambios, ejecutó las pruebas contra WildFly, ajusta el código en commits propios y defiende cada parte |
| Claude Code (agente verificador) | Claude Opus 5.5 | Revisión independiente del checkpoint: encontró 7 problemas (fuga de un cupo del bulkhead, replay con estado viejo, CI que no fallaba con tests de JS, evidencia ignorada por `.gitignore`, afirmaciones exageradas en los documentos) | Se corrigieron todos y se sumaron tests de regresión |
| Codex (OpenAI) | (el que asigna la CLI) | Propuesta de logo en HTML/CSS con la paleta de la marca (una G con un punto; finalmente no se usó) | Revisión visual y decisión de no usarlo |
| Codex (OpenAI) | (el que asigna la CLI) | Datos de 115 productos de ejemplo del catálogo (nombres, descripciones, precios, stock), con reglas fijadas por el equipo: marcas inventadas, rangos de precio, categorías y dibujos permitidos | Validación automática (ids, SKU, campos, marcas reales, rangos) y revisión de muestras antes de incorporarlos |
| Claude Code (Anthropic) | Claude Opus 5.5 | Adapter del modelo local (`ModeloLocal`, Ollama), prompt `busqueda-v3`, botón de búsqueda con IA, logo actual (bolsa en CSS) y 12 ilustraciones nuevas de productos | Pruebas en la tienda, eval set contra el modelo local (92 %) y revisión visual |
| Claude Code (Anthropic) con Claude in Chrome | Claude Opus 5.5 | Pruebas exploratorias de punta a punta en el navegador (41 pruebas, `INFORME-PRUEBAS.pdf`), 5 arreglos chicos que salieron de ahí y actualización de las capturas del manual | Pidió la prueba, revisó el informe y los arreglos antes de commitearlos |

**Reglas que seguimos** (Clase 14, diapositiva 9):
- Tareas chicas y con contexto; cada parte se probó con tests o contra WildFly antes de darla por buena.
- Ningún secreto ni dato real en los prompts: las claves se generan localmente (`.secretos/`, ignorado por git) y los datos son de prueba.
- No se aceptaron dependencias desconocidas: la única nueva es el SDK oficial de Anthropic para Java (`com.anthropic:anthropic-java`).
- Nada se declara como "probado" sin ejecución: lo que no se pudo verificar (el modelo de producción, Claude Haiku, sin API key) figura como pendiente en el RESUMEN y en el plan de pruebas.

**Responsabilidad:** el código entregado es del equipo. Cada integrante debe poder explicar la parte que expone y responder sobre código que no escribió (eliminatoria E8).

> Completar por el equipo: herramientas de IA usadas en las entregas anteriores (diseño del 06/11, 2.ª entrega), si las hubo, con el mismo formato.

## 2. IA como componente del producto

| Aspecto | Detalle |
|---|---|
| Feature | Búsqueda en lenguaje natural: la frase del cliente se convierte en filtros sugeridos del catálogo |
| Proveedor y modelo | Anthropic, `claude-haiku-5-5` (configurable con `GOMARKET_IA_MODELO`); justificación y costos en ADR-012 |
| Arquitectura | Detrás de `GatewayIA` (Facade + Adapter); la capa de negocio nunca llama al SDK |
| Datos que se envían | Sólo la frase de búsqueda, delimitada como dato. El gateway bloquea emails y números de tarjeta antes de enviar. No se envían datos de cuentas ni pedidos |
| Controles | Timeout de 5 s, circuit breaker, bulkhead, límite de uso por IP, salida validada contra el esquema y fallback por reglas |
| Humano en el circuito | El modelo sólo sugiere filtros; el usuario los ve como fichas y puede quitarlos. No compra, no cambia precios ni stock |
| Apagado | `GOMARKET_IA_HABILITADA=false` o el endpoint de administración: el sistema sigue con el fallback |
| Evidencia | Eval set versionado con umbrales que corre en el CI (ver `docs/ia/FICHA_IA.md`) |

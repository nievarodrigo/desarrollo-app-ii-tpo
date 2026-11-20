# Ficha de la feature de IA · Búsqueda en lenguaje natural

Gobernanza mínima que pide la cátedra (Clase 14, diapositiva 18), con el marco NIST AI RMF (Gobernar · Mapear · Medir · Gestionar).

## Ficha

| Campo | Contenido |
|---|---|
| Propósito | Convertir una frase del cliente ("auriculares baratos hasta 50 mil") en filtros del catálogo |
| Usuarios | Clientes de la tienda (anónimos o con sesión) |
| Tipo de uso | IA como **componente del producto**, detrás de `GatewayIA` (Facade + Adapter) |
| Proveedor y modelo | Producción: Anthropic, `claude-haiku-5-5` (configurable con `GOMARKET_IA_MODELO`). Desarrollo y demo: modelo local `qwen2.5:3b` con Ollama (`GOMARKET_IA_PROVEEDOR=local`). Justificación y costos en [ADR-012](../adr/adr-012.md) |
| Prompt | Versionado en el repo: `src/main/resources/ia/prompt-busqueda-v3.txt` (v1 y v2 quedan como historial; v3 agrega qué hay en cada categoría, que una sola palabra es una búsqueda y que la palabra clave de un objeto o actividad va en `q`, porque el modelo local mandaba "monitor" a Hogar y dejaba `q` vacío en "algo para tomar mate"). Consulta delimitada como dato, 9 ejemplos (uno de rechazo y uno de aclaración) |
| Contrato de salida | `SalidaModelo` (estado + criterios): JSON Schema derivado del record y **revalidado** en el servidor contra enums y rangos |
| Parámetros | Modelo, precios y habilitación por variables de entorno; `max_tokens` 1024; effort bajo; timeout HTTP 4,5 s sin reintentos |
| Límites conocidos | No conoce el catálogo real (sólo arma filtros); sin proveedor disponible responde el modo básico por reglas; evaluado con el modelo local, falta evaluar Haiku con una API key |

## Riesgos (OWASP LLM Top 10 2025) y mitigaciones

| Riesgo | Cómo se mitiga en GOMARKET | Evidencia |
|---|---|---|
| LLM01 Prompt injection | Política de entrada que rechaza patrones de inyección antes de llamar; consulta delimitada como dato; el modelo no tiene herramientas ni permisos | `GatewayIATest`, eval set EV-17 a EV-22 |
| LLM02 Divulgación de datos | Emails y números de tarjeta se bloquean antes de enviar (`aclarar`); no se envían datos de cuentas ni pedidos | EV-23 |
| LLM05 Salida mal manejada | La salida se valida campo por campo (enums, longitud, caracteres, rango de precio); una salida inventada se descarta y responde el fallback | `GatewayIATest › siElModeloDevuelveAlgoFueraDelContratoSeDescarta` |
| LLM06 Agencia excesiva | El modelo sólo sugiere filtros: no compra, no cambia precios ni stock. El usuario aplica o quita cada filtro | Diseño del endpoint y del front |
| LLM10 Consumo sin límite | Timeout 5 s, bulkhead de 4, circuit breaker, 30 consultas/min por IP, `max_tokens`, costo por consulta en las métricas | `LimiteDeConsultasTest`, `CircuitoIATest`, `GET /api/v1/admin/ia` |

## Política de datos

- **Se envía:** sólo la frase de búsqueda, delimitada.
- **No se envía:** emails, tarjetas, datos de cuenta, pedidos ni el JWT. Si la frase trae un email o una tarjeta, no sale del servidor.
- **Se guarda:** nada de la frase. El log registra versión del prompt, modelo, estado, origen, motivo, latencia, tokens y costo, **sin el texto de la consulta**.
- Marco legal: Ley 25.326 de datos personales.

## Humano en el circuito

La IA sugiere; el cliente decide. Si el texto que sugiere no encuentra productos, la tienda lo quita, conserva los demás filtros y lo avisa: un error del modelo nunca deja la pantalla vacía. Los filtros se muestran como fichas que se pueden quitar una por una, y la búsqueda común por texto sigue disponible. Ninguna decisión sobre personas, dinero o stock depende del modelo.

## Evidencia (Medir)

- **Tests deterministas:** `GatewayIATest`, `CircuitoIATest`, `ModeloClaudeTest`, `ModeloLocalTest`, `InterpreteDeReglasTest`, `LimiteDeConsultasTest`.
- **Eval set:** `src/test/resources/ia/eval-set-v1.json`, con 24 casos (8 adversariales) y umbrales de exactitud ≥ 85 %, adversariales resistidos 100 % y formato válido 100 %. Corre en cada build del CI (`EvalSetIATest`); si no cumple, el build falla. El resultado queda en `target/eval-ia.md` y en el resumen del job.
- **Último resultado** (19/11/2026, capa determinista, sin proveedor): exactitud 100 %, adversariales 100 %, formato 100 %. *Aclaración:* el dataset se armó junto con el intérprete de reglas, así que este número mide la política y el fallback, no la calidad del modelo.
- **Con un modelo real (local), 20/11/2026:** `GOMARKET_IA_PROVEEDOR=local ./mvnw test -Dtest=EvalSetIATest -Dia.eval.real=true` → 92 % de exactitud, 100 % de adversariales resistidos y 100 % de formato válido, ~0,4 s por consulta (`entrega-3/evidencias/eval-ia-modelo-local.txt`). Los ataques los frena la política del gateway antes de llegar al modelo. Falla en EV-07 ("monitor premium": acierta Tecnología pero ordena por valoración y no por precio) y EV-16 (sugiere en vez de pedir aclaración).
- **Con Claude Haiku:** el mismo comando con `ANTHROPIC_API_KEY` definida. Pendiente de una key.
- **Monitoreo en uso:** `GET /api/v1/admin/ia` (ADMIN) muestra consultas, origen, motivos de fallback, tasa de fallback, latencia p95, tokens y costo estimado.

## Plan de apagado (Gestionar)

- **Antes de arrancar:** `GOMARKET_IA_HABILITADA=false`.
- **En caliente, sin redeployar:** `PUT /api/v1/admin/ia/habilitada {"habilitada": false}` (ADMIN).
- **Automático:** el circuit breaker deja de llamar 30 s si fallan la mitad de las últimas 10 consultas.

En los tres casos la búsqueda sigue funcionando con el intérprete de reglas (`origen: "reglas"`).

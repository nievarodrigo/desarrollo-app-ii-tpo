# Tarea: 2.ª Entrega Parcial (13/11) de Desarrollo de Aplicaciones II, el núcleo funcionando

Proyecto GOMARKET (grupo 6), un e-commerce en Jakarta EE 10 (WAR, JAX-RS, JMS/MDB, JUnit 5), en este repo.
La consigna del profe está en .entrega2/consigna-entrega2.png (imagen). Los puntos son:
1. Componentes en capas desplegados y un primer flujo de punta a punta funcionando.
2. Patrones implementados en el código. El patrón State en la entidad central (Pedido) tiene que devolver HTTP 409 ante una transición inválida.
3. Seguridad base: autenticación con JWT y control de acceso por rol.
4. Mensajería funcionando en al menos un proceso (ya hay JMS con ProcesarPedidoMDB: verificalo y dejalo andando).
5. Primeros tests unitarios.
6. Jira al día. NO lo hagas vos: solo dejá en .entrega2/JIRA.md una lista de tarjetas sugeridas (título + descripción corta + estado), escritas en lenguaje simple, como de estudiantes universitarios, nada de jerga súper técnica.
7. Feedback del diseño incorporado: mirá docs/adr/, entrega-diseno/ (README, contratos, TPO_Diseno_Grupo6_Ecommerce.pdf) y los apuntes entrega-diseno/fuentes/Clase11_GOMARKET_Seguridad.docx.pdf y Clase12_GOMARKET_Patrones.docx.pdf. Alineá el código con lo que el diseño y esas clases piden. Si encontrás un feedback concreto del profe, incorporalo; si no lo hay, anotá en el resumen qué asumiste.

Contexto importante:
- Hay trabajo SIN commitear en main (State a medio hacer: EstadoPedido.java, EstadosPedido.java, EventoPedido.java, PedidoStateTest.java, cambios en Pedido, PedidoService, PedidoEnMemoria, ProcesarPedidoMDB, y docs/adr). Partí de eso, no lo pises ni lo tires.
- Antes de cambiar nada: `git switch -c entrega-2` (llevás los cambios sin commitear a esa rama). No hagas push, no toques main, no borres archivos de entrega/docs.
- No hay Java instalado y no hay sudo. Bajá un JDK 21 (Temurin) a ~/.local/jdk sin sudo y usalo con JAVA_HOME para compilar y correr `./mvnw test`. Si hace falta un servidor para el flujo punta a punta, preferí algo liviano que se pueda correr sin sudo (por ejemplo, tests de integración o un runtime embebible), y documentá cómo levantarlo.
- Mantené el estilo y los nombres en español del proyecto. Commits chicos y claros en español en la rama entrega-2.
- Todos los tests tienen que pasar al final.

Al terminar, escribí .entrega2/RESUMEN.md (simple, para estudiantes) con: qué quedó hecho por cada punto de la consigna, cómo probarlo (comandos curl de ejemplo para login JWT, flujo de pedido y el 409), qué tests hay, y qué falta o se asumió. Imprimí ese resumen como respuesta final.

## Agregados de Rodrigo
- NADA de SOAP. Es un e-commerce: todo REST/JSON. Si en src/ hay algo SOAP (JAX-WS, WSDL, clientes SOAP del "inventario legado"), sacalo del código o reemplazalo por un cliente/adaptador REST, y que el guion explique por qué elegimos REST y no SOAP. No borres los archivos de entrega-diseno/contratos (son de la entrega anterior), solo no los uses en el código.
- Al final, además de RESUMEN.md, armá un PDF de guion de defensa: .entrega2/Guion_Defensa_Entrega2_GOMARKET.pdf (y su fuente .md). Que diga qué decir punto por punto de la consigna, en lenguaje simple de estudiante, con las preguntas probables del profe y cómo responderlas (por qué State, por qué 409, cómo funciona el JWT y los roles, qué hace la mensajería, qué prueban los tests, por qué REST y no SOAP). Generá el PDF sin sudo (por ejemplo con pandoc si está, o Python con una librería instalada en un venv, o chromium/firefox headless desde HTML). Verificá que el PDF se abra y tenga el contenido.

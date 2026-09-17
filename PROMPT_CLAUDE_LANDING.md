# Prompt para Claude Code — landing del e-commerce

Copiar desde `/design` hasta el final y pegarlo en Claude Code desde la raíz del repositorio.

```text
/design

Trabajá en el repositorio ubicado en:
/home/rodrigo-nieva/Materias/DesarrolloAppII

Quiero que diseñes e implementes una landing page real para el e-commerce del Trabajo Práctico Integrador. IMPORTANTE: la página visible debe parecer una tienda online real; NO debe ser una página informativa sobre la materia, el TP, las capas, los patrones ni el equipo.

Antes de programar:
1. Inspeccioná `TPO/MOCKUP_TPO.md` para entender el dominio y los flujos.
2. Leé `TPO/TP_Integrador_DesarrolloAplicacionesII- Viernes.docx` y `TPO/Anexo_TP_Integrador_DesarrolloAplicacionesII.docx` solo para conocer restricciones del proyecto.
3. Inspeccioná `TPO/mockup-web`, pero no lo sobrescribas: ese sitio documenta el mockup anterior.
4. Creá la nueva landing en `TPO/landing-ecommerce/`.

Nombre de trabajo de la marca: **GOMARKET**. Podés mejorarlo visualmente, pero mantené ese nombre salvo que encuentres una alternativa claramente superior y la documentes.

Objetivo visual:
- estética premium, moderna y confiable;
- personalidad propia, evitando la apariencia genérica de template o de “sitio hecho por IA”;
- jerarquía tipográfica fuerte, espaciado consistente y excelente acabado;
- responsive real en desktop, tablet y móvil;
- accesibilidad: contraste, foco visible, navegación con teclado y HTML semántico;
- microinteracciones sutiles, sin animaciones excesivas.

La landing debe incluir como mínimo:
- header sticky con marca, navegación, búsqueda, cuenta y carrito;
- hero comercial con propuesta de valor y CTA “Comprar ahora”;
- categorías destacadas;
- grilla de productos destacados con imagen/placeholder cuidado, precio, stock, rating y botón de agregar;
- sección de beneficios: envío, pagos seguros, cambios/devoluciones y soporte;
- una promoción o banner editorial;
- testimonios o confianza social sin exageraciones falsas;
- newsletter;
- footer completo;
- drawer o panel de carrito funcional en frontend;
- búsqueda/filtros visuales básicos;
- estados hover, loading, vacío, sin stock y error;
- selector de tema `normal`, `claro` y `oscuro` en tiempo real, persistido localmente;
- contenido en español de Argentina y precios en ARS.

Implementación:
- construí una web funcional, no solo una imagen o descripción;
- usá HTML/CSS/JavaScript o el stack mínimo apropiado para un deploy estático rápido;
- no dependas de un backend todavía: usá datos mock separados y fáciles de reemplazar por una API Jakarta EE;
- no hardcodees secretos ni credenciales;
- evitá dependencias innecesarias y assets con licencias dudosas;
- agregá README con estructura, decisiones, ejecución local y cómo conectar luego el backend;
- agregá `vercel.json` si hace falta;
- no modifiques ni borres los documentos académicos ni el sitio `TPO/mockup-web`.

Validación obligatoria:
- comprobá que no haya errores de consola;
- probá navegación, carrito, buscador, selector de temas y responsive;
- verificá enlaces y botones;
- revisá visualmente desktop y mobile;
- corregí cualquier overflow o corte de texto.

Despliegue obligatorio:
1. Desde `TPO/landing-ecommerce`, ejecutá un deploy de producción en Vercel.
2. Usá la cuenta de Vercel ya autenticada en el entorno.
3. Verificá con una petición HTTP que la URL pública responda 200.
4. Verificá también que los assets carguen.
5. Registrá la URL final en `TPO/README.md` bajo “Landing e-commerce”.
6. Al terminar, informá la URL pública, los archivos creados y las validaciones realizadas.

No te detengas en una propuesta conceptual: diseñá, implementá, probá y desplegá.
```

---
title: "GOMARKET · guion de Rodrigo"
subtitle: "Explicación completa y simple para la presentación del 09/10"
author: "Rodrigo · líder del equipo"
date: "9 de octubre de 2026"
lang: es-AR
geometry: margin=2.2cm
fontsize: 11pt
---

## Para decir en voz alta (aprox. 3 minutos)

“Buen día. Soy Rodrigo y voy a presentar el avance de **GOMARKET**, nuestro
trabajo práctico de comercio electrónico. Somos cinco: **Iván** tiene como
área principal el backend, **Tomás** el frontend, **Fol** los datos,
**Nicolás** las pruebas, y yo coordino e integro el trabajo. Son los roles
que acordamos para organizarnos; todos tenemos que entender el recorrido
completo.

La tienda ya permitía ver productos, buscar, filtrarlos y armar un carrito.
Lo que **cambió ahora** es el paso siguiente. Antes, al confirmar, el
navegador hacía una simulación: inventaba un número de pedido y mostraba una
confirmación, pero el servidor no recibía nada. Ahora, cuando usamos la
versión que corre en WildFly, la página envía el pedido al servidor. El
servidor revisa qué productos se pidieron, si la cantidad entra en el stock
disponible y calcula el total por su cuenta. Eso importa porque el precio
que muestra el navegador puede modificarse; no debemos tomarlo como verdad.

Después de aceptar el pedido, el servidor lo deja en una **cola de
mensajes**. Piensen en una fila de tareas: una parte deja el pedido y otra
lo toma para trabajarlo. Esto viene de la **clase 8, de mensajería**. En
**nuestro negocio**, cada pedido es una tarea para **un solo procesador**:
por eso elegimos una **cola** y no un **tópico**, que repartiría la misma
noticia a varios sectores. No es que la cola sea siempre mejor; es la que
corresponde a este paso. La ventaja es que la página puede responder rápido sin quedarse
esperando todo el trabajo posterior. En la demostración se ve primero
‘Pedido recibido’ y luego podemos consultar si el mensaje llegó al
procesador. Ese cambio de estado prueba que la cola y el consumidor están
funcionando; no es sólo un dibujo.

La página y el servidor se comunican mediante una **API REST**: la página
envía el pedido y después consulta su estado. REST aparece como tema en el
calendario de **hoy, 09/10**; acá mostramos un uso concreto dentro del TP.
Como la página y el servidor son partes de nuestra propia tienda, es una
forma sencilla de comunicarlos. Lo importante para el usuario es que puede
enviar el pedido y recibir una respuesta clara.

En la **clase 9, sobre servicios web**, trabajamos también con **SOAP**.
Para nuestra tienda actual no encontramos un sistema externo que obligue a
usarlo. Por eso **no agregamos SOAP al código sólo para decir que está**.
Si un proveedor antiguo sólo ofreciera SOAP, entonces sí lo usaríamos para
consultar su stock. Es decir: la elección depende de **con quién necesita
comunicarse nuestro negocio**. Ese caso está diseñado como ejemplo, pero
no funciona todavía dentro de la tienda.

\newpage

Ahora muestro la prueba: agrego un producto al carrito, envío el pedido y
aparece como recibido. Después consulto el estado y se ve que el procesador
lo tomó. También probamos un pedido con más unidades de las disponibles: el
servidor lo rechaza. Nicolás puede explicar las pruebas; Iván
puede mostrar dónde se recibe y procesa el pedido; Tomás, lo que ve el
usuario; y Fol, los datos que viajan y lo que aún guardamos sólo en memoria.

Quiero cerrar con algo importante: **esto es un avance real, no una tienda
terminada**. Todavía no cobramos pagos, no reservamos stock definitivamente
y los pedidos no quedan guardados si se reinicia el servidor. Cuando el
sistema dice ‘procesado’, quiere decir que el consumidor recibió el mensaje,
**no** que la compra ya se pagó. Preferimos mostrar con claridad lo que sí
funciona y lo que falta para la siguiente etapa.”

## Si te hacen una pregunta rápida

- **¿Por qué una cola?** “Porque cada pedido debe atenderlo un solo
  procesador. La página no necesita esperar a que termine.”
- **¿Hay SOAP funcionando?** “No. Lo vimos en la clase 9 y diseñamos un caso
  posible para un proveedor antiguo, pero el negocio actual no lo necesita.”
- **¿Pedido procesado significa compra pagada?** “No. Significa que el
  consumidor recibió el mensaje; cobro y reserva de stock quedan pendientes.”

> **Nota para el equipo:** este guion reparte responsabilidades según los
> roles acordados; no afirma quién escribió cada archivo o commit. Si van a
> hablar de autoría individual, primero confírmenla entre ustedes.

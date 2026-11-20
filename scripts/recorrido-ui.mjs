// Recorrido de la tienda con Chrome sin interfaz (protocolo DevTools), contra WildFly corriendo.
// Hace lo mismo que un cliente: busca en lenguaje natural, ingresa, compra y consulta el pedido.
// Deja capturas reales en entrega-3/capturas/ (para el manual) y verifica cada paso.
//
//   google-chrome --headless=new --remote-debugging-port=9333 --user-data-dir=/tmp/gm-chrome about:blank &
//   node scripts/recorrido-ui.mjs
import { mkdirSync, writeFileSync } from 'node:fs';

const PUERTO = process.env.CDP_PORT ?? '9333';
const URL_TIENDA = process.env.TIENDA ?? 'http://localhost:8080/ecommerce/';
const SALIDA = new URL('../entrega-3/capturas/', import.meta.url);
mkdirSync(SALIDA, { recursive: true });

const destinos = await (await fetch(`http://127.0.0.1:${PUERTO}/json`)).json();
const pagina = destinos.find((d) => d.type === 'page');
const ws = new WebSocket(pagina.webSocketDebuggerUrl);
await new Promise((ok) => ws.addEventListener('open', ok, { once: true }));

let siguiente = 0;
const pendientes = new Map();
ws.addEventListener('message', (e) => {
  const m = JSON.parse(e.data);
  if (m.id && pendientes.has(m.id)) { pendientes.get(m.id)(m); pendientes.delete(m.id); }
});
const cdp = (method, params = {}) => new Promise((ok) => {
  const id = ++siguiente; pendientes.set(id, ok); ws.send(JSON.stringify({ id, method, params }));
});
const esperar = (ms) => new Promise((r) => setTimeout(r, ms));
const js = async (expr) => {
  const r = await cdp('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true });
  if (r.result?.exceptionDetails) throw new Error(r.result.exceptionDetails.exception?.description ?? expr);
  return r.result?.result?.value;
};
const captura = async (nombre) => {
  const r = await cdp('Page.captureScreenshot', { format: 'png' });
  writeFileSync(new URL(`${nombre}.png`, SALIDA), Buffer.from(r.result.data, 'base64'));
};
const resultados = [];
const verificar = (paso, ok, detalle) => { resultados.push({ paso, ok, detalle }); console.log(`${ok ? '✅' : '❌'} ${paso}: ${detalle}`); };
const avisos = () => js(`[...document.querySelectorAll('[role=status], .aviso, [data-avisos] *')].map(n => n.textContent.trim()).filter(Boolean).join(' | ')`);

await cdp('Page.enable');
await cdp('Emulation.setDeviceMetricsOverride', { width: 1366, height: 900, deviceScaleFactor: 1, mobile: false });
await cdp('Page.navigate', { url: URL_TIENDA });
await esperar(2500);
await js(`localStorage.clear(); sessionStorage.clear(); true`);
await cdp('Page.navigate', { url: URL_TIENDA });
await esperar(3000);
verificar('Catálogo', await js(`document.querySelectorAll('[data-agregar]').length > 0`), 'productos listados');
await captura('01-catalogo');

// 1. Búsqueda en lenguaje natural (gateway de IA)
await js(`(() => { const i = document.querySelector('[data-buscador-input]'); i.focus(); i.value = 'auriculares baratos hasta 300 mil';
  i.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true })); return true; })()`);
await esperar(2500);
const fichas = await js(`[...document.querySelectorAll('[data-fichas] *')].map(n => n.textContent.trim()).filter(Boolean).join(' · ')`);
verificar('Búsqueda con IA', /audio/i.test(fichas) || /300/.test(fichas), `fichas: ${fichas.slice(0, 120)}`);
await js(`window.scrollTo(0, 0); true`);
await esperar(500);
const hayResultados = await js(`document.querySelectorAll('[data-agregar]').length`);
verificar('Resultados de la búsqueda', hayResultados > 0, `${hayResultados} producto(s)`);
await captura('02-busqueda-lenguaje-natural');
await js(`document.querySelector('[data-limpiar-filtros]')?.click(); true`);
await esperar(1200);

// 2. Ingreso con la cuenta de prueba (pide también el JWT al backend)
await js(`document.querySelector('[data-cuenta-boton]').click(); true`);
await esperar(400);
await js(`document.querySelector('[data-cuenta-accion="ingresar"]')?.click(); true`);
await esperar(800);
await captura('03-ingreso');
await js(`(() => { document.querySelector('#ingreso-email').value = 'cliente@gomarket.com.ar';
  document.querySelector('#ingreso-clave').value = 'cliente123';
  document.querySelector('[data-cuenta-formulario="ingresar"]').requestSubmit(); return true; })()`);
await esperar(2500);
const token = await js(`sessionStorage.getItem('gomarket.tokenApi')`);
verificar('Ingreso', Boolean(token), token ? 'JWT del backend guardado en sessionStorage' : 'sin token');

// 3. Agregar al carrito y abrirlo
await js(`document.querySelector('[data-agregar]:not([disabled])').click(); true`);
await esperar(800);
await js(`document.querySelector('[data-abrir-carrito]').click(); true`);
await esperar(1000);
await captura('04-carrito');

// 4. Confirmar compra (REST con JWT + Idempotency-Key → cola JMS)
await js(`document.querySelector('[data-checkout]').click(); true`);
await esperar(3000);
const confirmado = await js(`document.querySelector('.carrito__confirmado')?.textContent.replace(/\\s+/g, ' ').trim() ?? ''`);
verificar('Compra', /Pedido (recibido|procesado)/.test(confirmado), confirmado.slice(0, 140) || (await avisos()));
await captura('05-pedido-recibido');

// 5. Consultar estado (el MDB ya lo procesó)
await js(`document.querySelector('[data-consultar-pedido]')?.click(); true`);
await esperar(2500);
const estado = await js(`document.querySelector('.carrito__confirmado h3')?.textContent ?? ''`);
verificar('Estado del pedido', /procesado/i.test(estado), estado || 'sin botón (ya estaba procesado)');
await captura('06-pedido-procesado');

writeFileSync(new URL('resultado.json', SALIDA), JSON.stringify(resultados, null, 2));
ws.close();
process.exit(resultados.every((r) => r.ok) ? 0 : 1);

#!/usr/bin/env python3
"""
Arma `figma-checkpoint-1.js`: un script de Figma Plugin API que crea los cuatro
frames desde los SVG y cablea las cuatro interacciones del prototipo.

Sirve igual en los dos runners:
  · Scripter (plugin de Figma Community, corre en el navegador) — pegar y ejecutar
  · figma_execute de un MCP puente — se ejecuta el mismo cuerpo
"""

import json
import os

RAIZ = os.path.dirname(os.path.abspath(__file__))

FRAMES = [
    ('catalogo', '01-catalogo-desktop.svg', 'Catálogo Desktop',            0, 0),
    ('detalle',  '02-detalle-desktop.svg',  'Detalle de producto Desktop', 1560, 0),
    ('mobile',   '03-catalogo-mobile.svg',  'Catálogo Mobile',             3120, 0),
    ('filtros',  '04-filtros-mobile.svg',   'Filtros Mobile',              3630, 0),
]

CABECERA = """/* ==========================================================================
   TPO Store — Checkpoint 1 · Catálogo
   Arma el prototipo completo: 4 frames + 4 interacciones + punto de inicio.

   Cómo usarlo con Scripter (plugin de Figma Community):
     1. Abrí el Design file donde querés el prototipo
     2. Plugins → Scripter
     3. Pegá TODO este archivo en el editor
     4. Play (▶)

   Generado por generar_script_figma.py — no editar a mano.
   ========================================================================== */

const FRAMES = __FRAMES__;

const DISOLVER = { type: 'DISSOLVE', easing: { type: 'EASE_OUT' }, duration: 0.2 };
const ENTRA = { type: 'MOVE_IN',  direction: 'BOTTOM', matchLayers: false,
                easing: { type: 'EASE_OUT' }, duration: 0.25 };
const SALE  = { type: 'MOVE_OUT', direction: 'BOTTOM', matchLayers: false,
                easing: { type: 'EASE_OUT' }, duration: 0.25 };

/* Origen · capa que dispara · destino · transición */
const CONEXIONES = [
  { origen: 'catalogo', capa: 'Componente / Product Card — Auriculares Cancelación Activa',
    destino: 'detalle',  transicion: DISOLVER },
  { origen: 'detalle',  capa: 'Hotspot / Cerrar detalle',
    destino: 'catalogo', transicion: DISOLVER },
  { origen: 'mobile',   capa: 'Hotspot / Abrir filtros',
    destino: 'filtros',  transicion: ENTRA },
  { origen: 'filtros',  capa: 'Hotspot / Aplicar filtros',
    destino: 'mobile',   transicion: SALE },
];

/* Scripter imprime con print(); un MCP con figma_execute usa console.log. */
const log = (typeof print === 'function') ? print : console.log;

/* --------------------------------------------------------------------- */

const nodos: any = {};

for (const f of FRAMES) {
  const n = figma.createNodeFromSvg(f.svg);   // devuelve un FrameNode ya armado
  n.name = f.name;
  n.x = f.x;
  n.y = f.y;
  n.clipsContent = true;
  figma.currentPage.appendChild(n);
  nodos[f.key] = n;
  log('frame creado: ' + f.name + '  ' + Math.round(n.width) + ' x ' + Math.round(n.height));
}

function buscar(raiz: any, nombre: string): any {
  if (raiz.name === nombre) return raiz;
  return raiz.findOne((n: any) => n.name === nombre);
}

for (const c of CONEXIONES) {
  const nodo = buscar(nodos[c.origen], c.capa);
  if (!nodo) {
    log('NO ENCONTRE la capa: ' + c.capa + ' dentro de ' + nodos[c.origen].name);
    continue;
  }
  const destinoId = nodos[c.destino].id;
  const accion = {
    type: 'NODE',
    destinationId: destinoId,
    navigation: 'NAVIGATE',
    transition: c.transicion,
    preserveScrollPosition: false,
  };
  /* La API nueva usa `actions: []`; las versiones viejas, `action: {}`.
     Se intenta la nueva y se cae a la vieja sin romper. */
  const nueva: any = { trigger: { type: 'ON_CLICK' }, actions: [accion] };
  const vieja: any = { trigger: { type: 'ON_CLICK' }, action: accion };
  try {
    if (nodo.setReactionsAsync) await nodo.setReactionsAsync([nueva]);
    else nodo.reactions = [nueva];
  } catch (e) {
    if (nodo.setReactionsAsync) await nodo.setReactionsAsync([vieja]);
    else nodo.reactions = [vieja];
  }
  log('OK conectado: ' + c.capa + '  ->  ' + nodos[c.destino].name);
}

figma.currentPage.name = 'Checkpoint 1 — Catálogo';
(figma.currentPage as any).flowStartingPoints = [
  { nodeId: nodos.catalogo.id, name: 'Checkpoint 1 — Catálogo' },
];

const creados: any = Object.keys(nodos).map((k) => nodos[k]);
figma.currentPage.selection = creados;
figma.viewport.scrollAndZoomIntoView(creados);
log('Listo. Punto de inicio: Catalogo Desktop.');
"""


def main():
    entradas = []
    for clave, archivo, nombre, x, y in FRAMES:
        svg = open(os.path.join(RAIZ, archivo), encoding='utf-8').read()
        entradas.append(
            '  {{ key: {}, name: {}, x: {}, y: {}, svg: {} }}'.format(
                json.dumps(clave), json.dumps(nombre, ensure_ascii=False), x, y,
                json.dumps(svg, ensure_ascii=False)))
    js = CABECERA.replace('__FRAMES__', '[\n' + ',\n'.join(entradas) + ',\n]')
    destino = os.path.join(RAIZ, 'figma-checkpoint-1.js')
    with open(destino, 'w', encoding='utf-8') as f:
        f.write(js)
    print(f'  figma-checkpoint-1.js  {os.path.getsize(destino) // 1024} KB')


if __name__ == '__main__':
    main()

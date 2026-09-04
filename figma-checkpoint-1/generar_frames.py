#!/usr/bin/env python3
"""
Genera los frames SVG del prototipo de Figma del Checkpoint 1 (componente Catalogo).

Los SVG estan pensados para importarse en Figma: sin CSS, sin <style>, sin filtros
y sin variables. Todo va en atributos de presentacion para que Figma los convierta
en capas vectoriales y de texto editables.

Fuente de datos: ../src/main/webapp/assets/data/catalogo.json
Fuente visual:   ../src/main/webapp/assets/css/styles.css (tokens)
                 ../src/main/webapp/assets/js/ui/art.js   (ilustraciones)
"""

import json
import os
import re

RAIZ = os.path.dirname(os.path.abspath(__file__))
TPO = os.path.dirname(RAIZ)
DATOS = os.path.join(TPO, 'src', 'main', 'webapp', 'assets', 'data', 'catalogo.json')

# --------------------------------------------------------------------------- #
# Tokens                                                                       #
# --------------------------------------------------------------------------- #
# Los valores rgba() del CSS se aplanan a hex sobre su fondo real: Figma edita
# mucho mejor un color solido que una capa con alpha heredada de un var().

FONDO        = '#F2EDE4'
FONDO_ALT    = '#E9E0D2'
PROFUNDO     = '#1E332C'
SUPERFICIE   = '#FBF8F2'
SUPERFICIE_2 = '#FFFFFF'
TINTA        = '#1A150F'
TINTA_2      = '#574C3D'
TINTA_3      = '#6B6153'
TINTA_INV    = '#F7F2E9'
BORDE        = '#DCD6CB'
BORDE_FUERTE = '#BCB4A6'
ACENTO       = '#BD3A1E'
ACENTO_TINTA = '#FFF6F1'
ACENTO_SUAVE = '#F5E5DD'
PROFUNDO_SUAVE = '#E5E4DE'
MOSTAZA      = '#C78B26'
EXITO        = '#1F6B4D'
PISTA        = '#E0D9CC'

ART_BG     = '#E6DCCC'
ART_SOFT   = '#C6C1B2'
ART_INK    = '#1A150F'
ART_ACCENT = '#BD3A1E'
ART_WARM   = '#C78B26'

UI      = 'Archivo, Inter, Helvetica, sans-serif'
DISPLAY = 'Fraunces, Georgia, serif'

CATEGORIAS_NOMBRE = {
    'tecnologia': 'Tecnología', 'audio': 'Audio', 'hogar': 'Hogar',
    'oficina': 'Oficina', 'deportes': 'Deportes'
}

# --------------------------------------------------------------------------- #
# Utilidades                                                                   #
# --------------------------------------------------------------------------- #

def esc(t):
    return (str(t).replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;'))


def escq(t):
    return esc(t).replace('"', '&quot;')


_ANCHO_CHAR = {
    'i': .28, 'j': .28, 'l': .28, 'I': .30, 't': .35, 'f': .34, 'r': .38,
    ' ': .26, '.': .28, ',': .28, ':': .28, ';': .28, "'": .22, '!': .30,
    '1': .48, 'm': .84, 'w': .76, 'M': .84, 'W': .92,
}


def ancho(texto, tam, peso=400, ls=0.0):
    """Ancho aproximado de un texto. Alcanza para dimensionar pills y wraps."""
    base = 0.545 if peso < 600 else 0.575
    total = 0.0
    for c in str(texto):
        f = _ANCHO_CHAR.get(c, base)
        if c.isupper() and c not in _ANCHO_CHAR:
            f = base + 0.075
        if c.isdigit() and c not in _ANCHO_CHAR:
            f = 0.565
        total += f
    return total * tam + ls * tam * max(0, len(str(texto)) - 1)


def envolver(texto, tam, peso, ancho_max, lineas_max=2):
    palabras = str(texto).split()
    lineas, actual = [], ''
    for p in palabras:
        prueba = (actual + ' ' + p).strip()
        if ancho(prueba, tam, peso) <= ancho_max or not actual:
            actual = prueba
        else:
            lineas.append(actual)
            actual = p
            if len(lineas) == lineas_max:
                break
    if actual and len(lineas) < lineas_max:
        lineas.append(actual)
    return lineas[:lineas_max]


def pesos(v):
    return '$ ' + f'{int(round(v)):,}'.replace(',', '.')


def numero(v):
    return f'{int(v):,}'.replace(',', '.')


def cuota(v, n=12):
    return pesos(round(v / n))


def descuento(actual, anterior):
    if not anterior or anterior <= actual:
        return 0
    return int(round((1 - actual / anterior) * 100))


# ------------------------------- primitivas -------------------------------- #

def g(nombre, cuerpo, transform=None, clip=None, opacity=None):
    a = f' id="{escq(nombre)}"'
    if transform:
        a += f' transform="{transform}"'
    if clip:
        a += f' clip-path="url(#{clip})"'
    if opacity is not None:
        a += f' opacity="{opacity}"'
    return f'<g{a}>{cuerpo}</g>'


def rect(x, y, w, h, r=0, fill='none', stroke=None, sw=1, opacity=None, nombre=None):
    a = f'<rect x="{x:g}" y="{y:g}" width="{w:g}" height="{h:g}"'
    if r:
        a += f' rx="{r:g}" ry="{r:g}"'
    a += f' fill="{fill}"'
    if stroke:
        a += f' stroke="{stroke}" stroke-width="{sw:g}"'
    if opacity is not None:
        a += f' opacity="{opacity:g}"'
    if nombre:
        a += f' id="{escq(nombre)}"'
    return a + '/>'


def linea(x1, y1, x2, y2, color=BORDE, sw=1):
    return (f'<line x1="{x1:g}" y1="{y1:g}" x2="{x2:g}" y2="{y2:g}" '
            f'stroke="{color}" stroke-width="{sw:g}"/>')


def circ(cx, cy, r, fill='none', stroke=None, sw=1, opacity=None):
    a = f'<circle cx="{cx:g}" cy="{cy:g}" r="{r:g}" fill="{fill}"'
    if stroke:
        a += f' stroke="{stroke}" stroke-width="{sw:g}"'
    if opacity is not None:
        a += f' opacity="{opacity:g}"'
    return a + '/>'


def texto(x, y, s, tam=14, peso=400, fill=TINTA, familia=UI, ls=None,
          anchor='start', italic=False, tachado=False, opacity=None):
    a = (f'<text x="{x:g}" y="{y:g}" font-family="{familia}" font-size="{tam:g}" '
         f'font-weight="{peso}" fill="{fill}"')
    if ls:
        a += f' letter-spacing="{ls:g}"'
    if anchor != 'start':
        a += f' text-anchor="{anchor}"'
    if italic:
        a += ' font-style="italic"'
    if tachado:
        a += ' text-decoration="line-through"'
    if opacity is not None:
        a += f' opacity="{opacity:g}"'
    return a + f' xml:space="preserve">{esc(s)}</text>'


def path(d, fill='none', stroke=None, sw=1, cap='round', join='round', opacity=None):
    a = f'<path d="{d}" fill="{fill}"'
    if stroke:
        a += (f' stroke="{stroke}" stroke-width="{sw:g}" '
              f'stroke-linecap="{cap}" stroke-linejoin="{join}"')
    if opacity is not None:
        a += f' opacity="{opacity:g}"'
    return a + '/>'


# --------------------------------- iconos ---------------------------------- #
# Todos dibujados sobre un lienzo de 24x24 y escalados en el punto de uso.

_ICONOS = {
    'lupa':    lambda c, w: (path('M10.8 10.8 m -7.2 0 a 7.2 7.2 0 1 0 14.4 0 a 7.2 7.2 0 1 0 -14.4 0', stroke=c, sw=w)
                             + path('M16.2 16.2 L21 21', stroke=c, sw=w)),
    'carro':   lambda c, w: (path('M3 4h2.2l2.3 11.2a1.6 1.6 0 0 0 1.6 1.3h8.3a1.6 1.6 0 0 0 1.6-1.2L21 8H6.2', stroke=c, sw=w)
                             + circ(10, 20, 1.5, fill=c) + circ(17.5, 20, 1.5, fill=c)),
    'cuenta':  lambda c, w: (circ(12, 8.5, 3.6, stroke=c, sw=w)
                             + path('M4.8 20a7.2 7.2 0 0 1 14.4 0', stroke=c, sw=w)),
    'menu':    lambda c, w: path('M4 7h16M4 12h16M4 17h16', stroke=c, sw=w),
    'filtros': lambda c, w: path('M3.6 6h16.8M7.2 12h9.6M10.2 18h3.6', stroke=c, sw=w),
    'cerrar':  lambda c, w: path('m7 7 10 10M17 7 7 17', stroke=c, sw=w),
    'chevron': lambda c, w: path('m7 10 5 5 5-5', stroke=c, sw=w),
    'flecha':  lambda c, w: path('M4 12h16M14 6l6 6-6 6', stroke=c, sw=w),
}


def icono(nombre, x, y, tam=24, color=TINTA, sw=1.9):
    s = tam / 24.0
    cuerpo = _ICONOS[nombre](color, sw / s)
    return f'<g transform="translate({x:g} {y:g}) scale({s:g})">{cuerpo}</g>'


def ancho_estrellas(tam, ls=None):
    ls = tam * 0.06 if ls is None else ls
    return 5 * 0.95 * tam + 4 * ls


def estrellas(x, y, tam=13, color=MOSTAZA):
    return texto(x, y, '★★★★★', tam, 400, color, ls=tam * 0.06)


# --------------------------------------------------------------------------- #
# Ilustraciones: puerto literal de assets/js/ui/art.js                          #
# --------------------------------------------------------------------------- #
# Mismo lienzo 320x240, mismas recetas, mismo hash de fondo por SKU. Lo unico
# que cambia es que las var(--art-*) quedan resueltas a hex y que se descarta el
# patron #grano (feTurbulence): Figma no importa filtros SVG.

def _semilla(t):
    h = 0
    for c in str(t):
        h = (h * 31 + ord(c)) % 100000
    return h


def _fondo(sku):
    s = _semilla(sku)
    rot = (s % 40) - 20
    cx = 90 + (s % 140)
    cy = 40 + (s % 60)
    v = s % 3
    if v == 0:
        trama = circ(cx, cy, 86, fill=ART_SOFT)
    elif v == 1:
        trama = (f'<rect x="{cx - 80}" y="{cy - 40}" width="170" height="170" rx="28" '
                 f'fill="{ART_SOFT}" transform="rotate({rot} {cx} {cy})"/>')
    else:
        trama = path(f'M {cx - 96} 200 Q {cx} {cy - 60} {cx + 96} 200 Z', fill=ART_SOFT)
    return rect(0, 0, 320, 240, fill=ART_BG) + trama


def _teclas():
    out = []
    for fila in range(4):
        for col in range(9):
            x = 60 + col * 24
            y = 108 + fila * 18
            dest = (fila == 1 and col == 3) or (fila == 2 and col == 6)
            out.append(rect(x, y, 18, 13, 3,
                            fill=ART_ACCENT if dest else ART_BG,
                            opacity=1 if dest else 0.75))
    return ''.join(out)


RECETAS = {
'headphones': lambda: (
    path('M78 148 V116 a82 82 0 0 1 164 0 v32', stroke=ART_INK, sw=13)
    + rect(56, 132, 44, 72, 20, fill=ART_ACCENT)
    + rect(220, 132, 44, 72, 20, fill=ART_ACCENT)
    + rect(68, 146, 20, 44, 10, fill=ART_BG, opacity=0.45)
    + circ(242, 168, 9, fill=ART_BG, opacity=0.45)),

'laptop': lambda: (
    rect(66, 62, 188, 118, 10, fill=ART_INK)
    + rect(78, 74, 164, 94, 4, fill=ART_ACCENT)
    + path('M92 148 L124 108 L150 138 L176 96 L212 148 Z', fill=ART_BG, opacity=0.5)
    + path('M44 180 h232 l14 20 H30 Z', fill=ART_INK)
    + rect(132, 186, 56, 6, 3, fill=ART_BG, opacity=0.5)),

'keyboard': lambda: (
    rect(40, 88, 240, 104, 14, fill=ART_INK)
    + rect(52, 100, 216, 80, 8, fill=ART_BG, opacity=0.12)
    + _teclas()
    + rect(112, 180, 96, 13, 4, fill=ART_BG, opacity=0.75)),

'lamp': lambda: (
    path('M74 200 h84', stroke=ART_INK, sw=12)
    + path('M116 200 V132 q0 -66 66 -66 h34', stroke=ART_INK, sw=11)
    + path('M186 58 h74 l-20 46 h-34 Z', fill=ART_ACCENT)
    + circ(223, 112, 9, fill=ART_WARM)
    + path('M199 122 L223 196 L247 122 Z', fill=ART_WARM, opacity=0.32)),

'speaker': lambda: (
    rect(96, 52, 128, 150, 30, fill=ART_INK)
    + circ(160, 104, 34, fill=ART_ACCENT)
    + circ(160, 104, 13, fill=ART_BG, opacity=0.5)
    + circ(160, 162, 20, fill=ART_BG, opacity=0.18)
    + rect(128, 196, 64, 10, 5, fill=ART_WARM)),

'chair': lambda: (
    path('M104 54 h96 a16 16 0 0 1 16 16 v66 a16 16 0 0 1 -16 16 h-96 a16 16 0 0 1 -16 -16 V70 a16 16 0 0 1 16 -16 Z', fill=ART_ACCENT)
    + path('M104 78 h96 M104 100 h96 M104 122 h96', stroke=ART_BG, sw=4, opacity=0.4)
    + rect(86, 152, 132, 20, 10, fill=ART_INK)
    + path('M152 172 v24 M112 216 l40 -20 l40 20', stroke=ART_INK, sw=10)),

'coffee': lambda: (
    path('M126 24 q10 -13 0 -24 M154 24 q10 -13 0 -24 M182 24 q10 -13 0 -24', stroke=ART_WARM, sw=6)
    + rect(104, 42, 104, 22, 8, fill=ART_INK)
    + rect(140, 30, 32, 14, 6, fill=ART_INK)
    + path('M112 64 h88 l-11 118 a14 14 0 0 1 -14 13 h-38 a14 14 0 0 1 -14 -13 Z', fill=ART_ACCENT)
    + path('M118 122 h76 l-6 60 a14 14 0 0 1 -14 13 h-36 a14 14 0 0 1 -14 -13 Z', fill=ART_INK, opacity=0.35)
    + path('M200 82 q34 6 34 34 t-36 32', stroke=ART_INK, sw=11)
    + rect(98, 195, 116, 13, 6, fill=ART_INK)),

'dumbbell': lambda: (
    rect(128, 110, 64, 20, 10, fill=ART_INK)
    + rect(96, 82, 30, 76, 10, fill=ART_ACCENT)
    + rect(194, 82, 30, 76, 10, fill=ART_ACCENT)
    + rect(66, 96, 26, 48, 9, fill=ART_INK)
    + rect(228, 96, 26, 48, 9, fill=ART_INK)
    + path('M84 186 h152', stroke=ART_WARM, sw=10)),

'monitor': lambda: (
    rect(46, 48, 228, 132, 12, fill=ART_INK)
    + rect(58, 60, 204, 108, 5, fill=ART_ACCENT)
    + rect(76, 80, 72, 10, 5, fill=ART_BG, opacity=0.6)
    + rect(76, 100, 128, 8, 4, fill=ART_BG, opacity=0.35)
    + rect(76, 118, 104, 8, 4, fill=ART_BG, opacity=0.35)
    + rect(146, 180, 28, 24, fill=ART_INK)
    + rect(106, 200, 108, 12, 6, fill=ART_INK)),

'bottle': lambda: (
    rect(140, 30, 40, 26, 6, fill=ART_INK)
    + path('M132 56 h56 q22 0 22 26 v100 q0 26 -26 26 h-48 q-26 0 -26 -26 V82 q0 -26 22 -26 Z', fill=ART_ACCENT)
    + path('M138 96 h44', stroke=ART_BG, sw=7, opacity=0.5)
    + path('M138 116 h30', stroke=ART_BG, sw=7, opacity=0.3)
    + rect(110, 140, 100, 26, fill=ART_WARM, opacity=0.55)),

'desk': lambda: (
    rect(42, 96, 236, 18, 6, fill=ART_WARM)
    + rect(66, 114, 20, 86, 6, fill=ART_INK)
    + rect(234, 114, 20, 86, 6, fill=ART_INK)
    + path('M56 200 h40 M224 200 h40', stroke=ART_INK, sw=11)
    + rect(122, 52, 76, 44, 6, fill=ART_ACCENT)
    + path('M282 122 v-30 l-14 12 M282 92 l14 12', stroke=ART_ACCENT, sw=7)),

'watch': lambda: (
    rect(132, 26, 56, 52, 18, fill=ART_INK)
    + rect(132, 162, 56, 52, 18, fill=ART_INK)
    + rect(110, 62, 100, 118, 28, fill=ART_INK)
    + rect(122, 74, 76, 94, 20, fill=ART_ACCENT)
    + path('M136 132 l14 -20 l14 26 l14 -38 l14 32', stroke=ART_BG, sw=6)
    + rect(210, 102, 9, 26, 4, fill=ART_WARM)),

'blanket': lambda: (
    path('M56 78 q52 -26 104 0 t104 0 v96 q-52 26 -104 0 t-104 0 Z', fill=ART_ACCENT)
    + path('M56 110 q52 -26 104 0 t104 0', stroke=ART_BG, sw=6, opacity=0.4)
    + path('M56 142 q52 -26 104 0 t104 0', stroke=ART_BG, sw=6, opacity=0.4)
    + path('M56 174 v30 q52 26 104 0 t104 0 v-30', fill=ART_WARM, opacity=0.6)),

'mic': lambda: (
    rect(132, 34, 56, 106, 28, fill=ART_ACCENT)
    + path('M140 52 h40 M140 68 h40 M140 84 h40 M140 100 h40', stroke=ART_BG, sw=5, opacity=0.45)
    + path('M104 116 a56 56 0 0 0 112 0', stroke=ART_INK, sw=11)
    + path('M160 172 v28', stroke=ART_INK, sw=11)
    + path('M116 208 l44 -20 l44 20', stroke=ART_INK, sw=10)),

'backpack': lambda: (
    path('M120 62 a40 40 0 0 1 80 0', stroke=ART_INK, sw=12)
    + path('M96 82 h128 q22 0 22 24 v82 q0 22 -22 22 h-128 q-22 0 -22 -22 v-82 q0 -24 22 -24 Z', fill=ART_ACCENT)
    + rect(106, 128, 108, 46, 12, fill=ART_BG, opacity=0.28)
    + rect(140, 96, 40, 18, 9, fill=ART_WARM)
    + path('M96 174 h128', stroke=ART_INK, sw=6, opacity=0.35)),

'organizer': lambda: (
    rect(52, 112, 216, 80, 10, fill=ART_WARM)
    + rect(66, 126, 58, 52, 6, fill=ART_BG, opacity=0.35)
    + rect(134, 126, 58, 52, 6, fill=ART_BG, opacity=0.35)
    + rect(202, 126, 52, 52, 6, fill=ART_BG, opacity=0.35)
    + rect(80, 62, 10, 56, 5, fill=ART_ACCENT)
    + rect(98, 76, 10, 42, 5, fill=ART_INK)
    + path('M212 74 q22 12 0 24 t0 24', stroke=ART_INK, sw=7)),
}

_CLIPS = []


def _clip(uid, x, y, w, h, r=0, solo_arriba=False):
    """Registra un clipPath y devuelve su id."""
    if solo_arriba:
        d = (f'M{x} {y + h} V{y + r} a{r} {r} 0 0 1 {r} -{r} '
             f'h{w - 2 * r} a{r} {r} 0 0 1 {r} {r} V{y + h} Z')
        forma = path(d, fill='#000')
    else:
        forma = rect(x, y, w, h, r, fill='#000')
    _CLIPS.append(f'<clipPath id="{uid}">{forma}</clipPath>')
    return uid


def arte(uid, x, y, w, h, prod, r=0, solo_arriba=False, modo='cover'):
    """Bloque de ilustracion recortado a una caja, con el mismo encuadre que la web."""
    cid = _clip(uid, x, y, w, h, r, solo_arriba)
    if modo == 'cover':
        s = max(w / 320.0, h / 240.0)
    else:
        s = min(w / 320.0, h / 240.0)
    tx = x + (w - 320 * s) / 2.0
    ty = y + (h - 240 * s) / 2.0
    receta = RECETAS.get(prod['arte'], RECETAS['headphones'])
    cuerpo = _fondo(prod['sku']) + g('objeto', receta())
    return (f'<g clip-path="url(#{cid})">'
            + rect(x, y, w, h, fill=ART_BG)
            + f'<g transform="translate({tx:.3f} {ty:.3f}) scale({s:.4f})">{cuerpo}</g>'
            + '</g>')


# --------------------------------------------------------------------------- #
# Componentes reutilizables                                                    #
# --------------------------------------------------------------------------- #

def pill(x, y, etiqueta, tam=12, peso=600, bg=ACENTO_SUAVE, fg=ACENTO,
         alto=26, pad=11, r=None, borde=None, ls=0.0):
    """Chip / marbete. Devuelve (markup, ancho)."""
    w = ancho(etiqueta, tam, peso, ls) + pad * 2
    rr = alto / 2 if r is None else r
    cuerpo = rect(x, y, w, alto, rr, fill=bg, stroke=borde, sw=1)
    cuerpo += texto(x + w / 2, y + alto / 2 + tam * 0.36, etiqueta, tam, peso, fg,
                    anchor='middle', ls=ls or None)
    return cuerpo, w


def chip_filtro(x, y, etiqueta, con_cierre=True, tam=12.5, alto=32):
    w = ancho(etiqueta, tam, 500) + 22 + (18 if con_cierre else 0)
    out = rect(x, y, w, alto, alto / 2, fill=SUPERFICIE, stroke=BORDE_FUERTE, sw=1)
    out += texto(x + 13, y + alto / 2 + tam * 0.36, etiqueta, tam, 500, TINTA_2)
    if con_cierre:
        out += icono('cerrar', x + w - 26, y + alto / 2 - 7, 14, TINTA_3, 2.1)
    return out, w


def boton(x, y, w, h, etiqueta, variante='primario', tam=14.5, peso=600, icono_izq=None):
    if variante == 'primario':
        bg, fg, br = ACENTO, ACENTO_TINTA, None
    elif variante == 'profundo':
        bg, fg, br = PROFUNDO, TINTA_INV, None
    elif variante == 'inhabilitado':
        bg, fg, br = PISTA, TINTA_3, None
    else:  # fantasma / secundario
        bg, fg, br = 'none', TINTA, BORDE_FUERTE
    out = rect(x, y, w, h, min(14, h / 2.6), fill=bg, stroke=br, sw=1.2)
    cx = x + w / 2
    if icono_izq:
        aw = ancho(etiqueta, tam, peso)
        ix = cx - (aw + 24) / 2
        out += icono(icono_izq, ix, y + h / 2 - 9, 18, fg, 2)
        out += texto(ix + 26, y + h / 2 + tam * 0.36, etiqueta, tam, peso, fg)
    else:
        out += texto(cx, y + h / 2 + tam * 0.36, etiqueta, tam, peso, fg, anchor='middle')
    return out


def buscador(x, y, w, h, marcador, tam=14, bg=SUPERFICIE_2):
    out = rect(x, y, w, h, h / 2, fill=bg, stroke=BORDE, sw=1)
    out += icono('lupa', x + 15, y + h / 2 - 9, 18, TINTA_3, 2)
    out += texto(x + 43, y + h / 2 + tam * 0.36, marcador, tam, 400, TINTA_3)
    return out


def marca(x, y, escala=1.0):
    """Sello + logotipo de TPO Store (tomado de index.html)."""
    lado = 34 * escala
    sello = rect(x, y, lado, lado, 10 * escala, fill=PROFUNDO)
    s = lado / 32.0 * 0.72
    off = (lado - 32 * s) / 2
    sello += (f'<g transform="translate({x + off:.2f} {y + off:.2f}) scale({s:.4f})">'
              + path('M6 24V8h8a5 5 0 0 1 0 10H6', stroke=TINTA_INV, sw=3.4, cap='square')
              + circ(24, 22, 4, fill=TINTA_INV) + '</g>')
    t = 19 * escala
    bx = x + lado + 10 * escala
    by = y + lado / 2 + t * 0.35
    sello += texto(bx, by, 'TPO', t, 700, TINTA, ls=t * 0.01)
    sello += texto(bx + ancho('TPO', t, 700) + 5 * escala, by, 'Store', t, 500,
                   ACENTO, familia=DISPLAY, italic=True)
    return g('Componente / Header — Marca', sello)


def interruptor(x, y, w=48, h=28, encendido=False):
    knob = w - h / 2 - 3 if encendido else h / 2 + 3
    return (rect(x, y, w, h, h / 2, fill=PROFUNDO if encendido else PISTA)
            + circ(x + knob, y + h / 2, h / 2 - 4, fill=SUPERFICIE_2))


def selector(x, y, w, h, valor, tam=14):
    out = rect(x, y, w, h, 12, fill=FONDO, stroke=BORDE, sw=1)
    out += texto(x + 16, y + h / 2 + tam * 0.36, valor, tam, 500, TINTA)
    out += icono('chevron', x + w - 34, y + h / 2 - 10, 20, TINTA_3, 2)
    return out


def puntaje(x, y, prod, tam=12.5):
    out = estrellas(x, y, tam)
    dx = ancho_estrellas(tam) + 8
    out += texto(x + dx, y, f"{prod['rating']:.1f}", tam, 700, TINTA)
    dx += ancho(f"{prod['rating']:.1f}", tam, 700) + 6
    out += texto(x + dx, y, f"({numero(prod['resenias'])})", tam - 0.5, 400, TINTA_3)
    return out


def texto_stock(prod):
    if prod['stock'] == 0:
        return 'Sin stock por ahora', TINTA_3
    if prod['stock'] <= 5:
        return f"¡Quedan {prod['stock']}!", ACENTO
    return f"{prod['stock']} disponibles", EXITO


def marbetes(x, y, prod, tam=11.5, alto=24):
    """Marbetes de oferta / destacado / agotado, apilados en la esquina del arte."""
    out, dx = '', 0
    if prod['stock'] == 0:
        m, w = pill(x, y, 'Sin stock', tam, 700, TINTA, TINTA_INV, alto, 10)
        out += m
        dx = w + 6
    elif prod.get('precioAnterior'):
        d = descuento(prod['precio'], prod['precioAnterior'])
        m, w = pill(x, y, f'−{d} %', tam, 700, ACENTO, ACENTO_TINTA, alto, 10)
        out += m
        dx = w + 6
    if 'destacado' in prod.get('etiquetas', []):
        m, _ = pill(x + dx, y, 'Destacado', tam, 700, PROFUNDO, TINTA_INV, alto, 10)
        out += m
    return out


# ------------------------------- Product Card ------------------------------ #

CARD_W, CARD_H, CARD_MEDIA = 304, 458, 228


def tarjeta_desktop(uid, x, y, prod):
    b = y + CARD_MEDIA           # inicio del cuerpo
    out = rect(x, y, CARD_W, CARD_H, 18, fill=SUPERFICIE, stroke=BORDE, sw=1)
    out += arte(uid, x + 1, y + 1, CARD_W - 2, CARD_MEDIA - 1, prod, r=17, solo_arriba=True)
    if prod['stock'] == 0:
        out += rect(x + 1, y + 1, CARD_W - 2, CARD_MEDIA - 1, fill=ART_BG, opacity=0.5)
    out += linea(x, b, x + CARD_W, b, BORDE)
    out += marbetes(x + 14, y + 14, prod)

    # boton flotante "Ver detalle" — es el hotspot del prototipo
    bw, bh = 104, 30
    bx, by_ = x + CARD_W - 14 - bw, y + CARD_MEDIA - 14 - bh
    out += rect(bx, by_, bw, bh, 15, fill=SUPERFICIE_2, stroke=BORDE, sw=1)
    out += texto(bx + bw / 2, by_ + bh / 2 + 4.5, 'Ver detalle', 12.5, 600, TINTA,
                 anchor='middle')

    ix, iw = x + 16, CARD_W - 32
    meta = f"{prod['marca']}  ·  {CATEGORIAS_NOMBRE.get(prod['categoria'], prod['categoria'])}"
    out += texto(ix, b + 24, meta.upper(), 10.5, 600, TINTA_3, ls=1.0)

    for i, ln in enumerate(envolver(prod['nombre'], 16.5, 600, iw, 2)):
        out += texto(ix, b + 50 + i * 20, ln, 16.5, 600, TINTA)

    out += puntaje(ix, b + 96, prod)
    st, sc = texto_stock(prod)
    out += circ(x + CARD_W - 16 - ancho(st, 12, 600) - 12, b + 92, 3.5, fill=sc)
    out += texto(x + CARD_W - 16, b + 96, st, 12, 600, sc, anchor='end')

    out += texto(ix, b + 130, pesos(prod['precio']), 21, 700, TINTA)
    if prod.get('precioAnterior'):
        out += texto(ix, b + 152, pesos(prod['precioAnterior']), 13, 400, TINTA_3,
                     tachado=True)
        p, _ = pill(ix + ancho(pesos(prod['precioAnterior']), 13) + 10, b + 141,
                    f"{descuento(prod['precio'], prod['precioAnterior'])} % OFF",
                    11, 700, ACENTO_SUAVE, ACENTO, 20, 8)
        out += p
    out += texto(ix, b + 172, f"12x {cuota(prod['precio'])} sin interés", 12, 400, TINTA_2)

    agotado = prod['stock'] == 0
    out += boton(ix, b + 182, iw, 40,
                 'Sin stock' if agotado else 'Agregar al carrito',
                 'inhabilitado' if agotado else 'primario', 14)
    return g(f"Componente / Product Card — {prod['nombre']}", out)


MCARD_W, MCARD_H, MCARD_MEDIA = 171, 300, 128


def tarjeta_mobile(uid, x, y, prod):
    b = y + MCARD_MEDIA
    out = rect(x, y, MCARD_W, MCARD_H, 14, fill=SUPERFICIE, stroke=BORDE, sw=1)
    out += arte(uid, x + 1, y + 1, MCARD_W - 2, MCARD_MEDIA - 1, prod, r=13, solo_arriba=True)
    out += linea(x, b, x + MCARD_W, b, BORDE)
    out += marbetes(x + 9, y + 9, prod, tam=10, alto=20)

    ix, iw = x + 12, MCARD_W - 24
    meta = f"{prod['marca']}  ·  {CATEGORIAS_NOMBRE.get(prod['categoria'], prod['categoria'])}"
    out += texto(ix, b + 20, meta.upper(), 9, 600, TINTA_3, ls=0.7)
    for i, ln in enumerate(envolver(prod['nombre'], 12.5, 600, iw, 2)):
        out += texto(ix, b + 40 + i * 16, ln, 12.5, 600, TINTA)
    out += texto(ix, b + 82, pesos(prod['precio']), 16.5, 700, TINTA)
    if prod.get('precioAnterior'):
        out += texto(ix, b + 100, pesos(prod['precioAnterior']), 10.5, 400, TINTA_3,
                     tachado=True)
        out += texto(ix + ancho(pesos(prod['precioAnterior']), 10.5) + 8, b + 100,
                     f"−{descuento(prod['precio'], prod['precioAnterior'])} %",
                     10.5, 700, ACENTO)
    else:
        out += texto(ix, b + 100, f"12x {cuota(prod['precio'])}", 10.5, 400, TINTA_2)
    st, sc = texto_stock(prod)
    out += texto(ix, b + 118, st, 10.5, 600, sc)
    agotado = prod['stock'] == 0
    out += boton(ix, b + 126, iw, 34,
                 'Sin stock' if agotado else 'Agregar',
                 'inhabilitado' if agotado else 'primario', 13)
    return g(f"Componente / Product Card mobile — {prod['nombre']}", out)


# ---------------------------------- Header --------------------------------- #

def anuncio(w, h=34, tam=11.5):
    txt = ('Envío gratis desde $ 150.000   ·   12 cuotas sin interés   ·   '
           'Cambios sin cargo dentro de los 30 días   ·   Retiro en sucursal en 24 h')
    if w < 600:
        txt = 'Envío gratis desde $ 150.000   ·   12 cuotas sin interés'
    out = rect(0, 0, w, h, fill=PROFUNDO)
    out += texto(w / 2, h / 2 + tam * 0.36, txt, tam, 500, TINTA_INV,
                 anchor='middle', ls=0.4)
    return g('Barra de anuncios', out)


def header_desktop(y0=34, w=1440, h=68):
    out = rect(0, y0, w, h, fill=SUPERFICIE)
    out += linea(0, y0 + h, w, y0 + h, BORDE)
    out += marca(80, y0 + 17)

    items = ['Catálogo', 'Categorías', 'Ofertas', 'Beneficios', 'Ayuda']
    x = 268
    nav = ''
    for it in items:
        activo = it == 'Catálogo'
        nav += texto(x, y0 + 42, it, 14, 600 if activo else 500,
                     TINTA if activo else TINTA_2)
        aw = ancho(it, 14, 600 if activo else 500)
        if activo:
            nav += rect(x, y0 + 50, aw, 2.5, 1.25, fill=ACENTO)
        x += aw + 26
    out += g('Componente / Header — Navegación', nav)

    out += g('Componente / Campo de búsqueda — Cabecera',
             buscador(700, y0 + 14, 316, 40, 'Buscar auriculares, monitores, sillas…'))

    # selector de tema (normal / claro / oscuro), tal cual la implementacion
    temas = rect(1042, y0 + 18, 86, 32, 16, fill=FONDO, stroke=BORDE, sw=1)
    for i, c in enumerate([FONDO_ALT, '#FFFFFF', '#131110']):
        cx = 1058 + i * 26
        temas += circ(cx, y0 + 34, 9, fill=c, stroke=BORDE_FUERTE, sw=1)
        if i == 0:
            temas += circ(cx, y0 + 34, 12, stroke=ACENTO, sw=2)
    out += g('Selector de tema', temas)

    acc = icono('cuenta', 1148, y0 + 24, 20, TINTA, 1.9)
    acc += texto(1174, y0 + 42, 'Cuenta', 13.5, 500, TINTA)
    acc += icono('carro', 1258, y0 + 24, 20, TINTA, 1.9)
    acc += texto(1284, y0 + 42, 'Carrito', 13.5, 500, TINTA)
    out += g('Acciones de cabecera', acc)
    return g('Componente / Header — Desktop', out)


def header_mobile(y0=26, w=390, h=56):
    out = rect(0, y0, w, h, fill=SUPERFICIE)
    out += linea(0, y0 + h, w, y0 + h, BORDE)
    out += marca(16, y0 + 12, escala=0.92)
    acc = icono('cuenta', 268, y0 + 18, 22, TINTA, 1.9)
    acc += icono('carro', 306, y0 + 18, 22, TINTA, 1.9)
    acc += circ(325, y0 + 20, 7.5, fill=ACENTO)
    acc += texto(325, y0 + 23, '0', 9.5, 700, ACENTO_TINTA, anchor='middle')
    acc += icono('menu', 344, y0 + 18, 22, TINTA, 1.9)
    out += g('Acciones de cabecera mobile', acc)
    return g('Componente / Header — Mobile', out)


# ------------------------------ Panel de filtros --------------------------- #

PANEL_X, PANEL_Y, PANEL_W, PANEL_H = 80, 214, 296, 666
PRECIO_MIN, PRECIO_MAX = 38900, 1849000


def rotulo(x, y, s):
    return texto(x, y, s.upper(), 11, 700, TINTA_3, ls=1.3)


def panel_filtros(cats):
    x, y = PANEL_X, PANEL_Y
    ix, iw = x + 22, PANEL_W - 44
    out = rect(x, y, PANEL_W, PANEL_H, 20, fill=SUPERFICIE, stroke=BORDE, sw=1)

    out += rotulo(ix, y + 36, 'Buscar')
    out += g('Componente / Campo de búsqueda — Panel',
             buscador(ix, y + 46, iw, 40, 'Nombre, marca o SKU', 13.5, FONDO))

    out += rotulo(ix, y + 116, 'Categoría')
    filas = ''
    for i, (etq, n) in enumerate(cats):
        fy = y + 126 + i * 36
        sel = i == 0
        if sel:
            filas += rect(ix, fy, iw, 32, 10, fill=ACENTO_SUAVE)
        filas += circ(ix + 16, fy + 16, 7.5,
                      fill=ACENTO if sel else 'none',
                      stroke=None if sel else BORDE_FUERTE, sw=1.6)
        if sel:
            filas += circ(ix + 16, fy + 16, 3, fill=ACENTO_TINTA)
        filas += texto(ix + 32, fy + 20.5, etq, 13.5, 600 if sel else 500,
                       TINTA if sel else TINTA_2)
        filas += texto(ix + iw - 12, fy + 20.5, str(n), 12, 500, TINTA_3, anchor='end')
    out += g('Componente / Control de categoría', filas)

    out += rotulo(ix, y + 372, 'Precio máximo')
    sl = rect(ix, y + 386, iw, 6, 3, fill=PISTA)
    sl += rect(ix, y + 386, iw * 0.87, 6, 3, fill=ACENTO)
    sl += circ(ix + iw * 0.87, y + 389, 11, fill=SUPERFICIE_2, stroke=ACENTO, sw=3)
    sl += texto(ix, y + 424, 'Hasta', 13, 400, TINTA_2)
    sl += texto(ix + 40, y + 424, pesos(PRECIO_MAX), 13.5, 700, TINTA)
    sl += texto(ix + iw, y + 424, f'mín. {pesos(PRECIO_MIN)}', 11.5, 400, TINTA_3,
                anchor='end')
    out += g('Control de precio máximo', sl)

    out += rotulo(ix, y + 456, 'Disponibilidad')
    sw_ = interruptor(ix, y + 466, 48, 28, encendido=False)
    sw_ += texto(ix + 60, y + 485, 'Sólo con stock', 13.5, 500, TINTA)
    out += g('Componente / Control de stock', sw_)

    out += rotulo(ix, y + 526, 'Ordenar por')
    out += g('Control de ordenamiento', selector(ix, y + 536, iw, 44, 'Relevancia'))

    out += boton(ix, y + 600, iw, 42, 'Limpiar filtros', 'fantasma', 14)
    return g('Componente / Panel de filtros', out)


# --------------------------------------------------------------------------- #
# Frames                                                                       #
# --------------------------------------------------------------------------- #

W_D, H_D = 1440, 1024
W_M, H_M = 390, 844

COLS = [408, 732, 1056]
FILAS = [214, 692]


def frame_catalogo_desktop(datos, pfx='d'):
    prods = datos['productos'][:6]
    cats = [('Todas', 16)] + [
        (CATEGORIAS_NOMBRE[c['slug']],
         sum(1 for p in datos['productos'] if p['categoria'] == c['slug']))
        for c in datos['categorias']]

    out = rect(0, 0, W_D, H_D, fill=FONDO, nombre='Fondo')
    out += anuncio(W_D)
    out += header_desktop()

    enc = texto(80, 138, 'EL CATÁLOGO', 11.5, 700, ACENTO, ls=1.7)
    enc += texto(80, 182, 'Lo que tenemos hoy', 38, 700, TINTA, familia=DISPLAY)
    enc += texto(1360, 142, '16 productos en 5 categorías', 13.5, 500, TINTA_2,
                 anchor='end')
    out += g('Encabezado del catálogo', enc)

    # fichas activas (chips) alineadas a la derecha
    piezas = [chip_filtro(0, 0, f'Hasta {pesos(PRECIO_MAX)}'),
              chip_filtro(0, 0, 'Relevancia')]
    total = sum(p[1] for p in piezas) + 10 * (len(piezas) - 1)
    cx = 1360 - total
    fichas = texto(cx - 14, 178, 'Limpiar', 12.5, 600, ACENTO, anchor='end')
    for _, wdt in piezas:
        pass
    for etq, wdt in zip([f'Hasta {pesos(PRECIO_MAX)}', 'Relevancia'],
                        [p[1] for p in piezas]):
        m, _ = chip_filtro(cx, 158, etq)
        fichas += m
        cx += wdt + 10
    out += g('Componente / Chip de filtro — Fichas activas', fichas)

    out += panel_filtros(cats)

    grilla = ''
    for i, p in enumerate(prods):
        grilla += tarjeta_desktop(f'{pfx}-card-{i}', COLS[i % 3], FILAS[i // 3], p)
    out += g('Grilla de productos', grilla)
    return out


def frame_detalle_desktop(datos, pfx='m'):
    prod = datos['productos'][0]
    out = frame_catalogo_desktop(datos, pfx)
    out += rect(0, 0, W_D, H_D, fill=TINTA, opacity=0.58, nombre='Scrim')

    mx, my, mw, mh = 230, 232, 980, 560
    m = rect(mx, my, mw, mh, 24, fill=SUPERFICIE, stroke=BORDE, sw=1)
    m += arte(f'{pfx}-detalle', mx + 28, my + 28, 420, 315, prod, r=18)
    m += marbetes(mx + 44, my + 44, prod, tam=12, alto=26)

    # ficha tecnica debajo de la ilustracion
    fx, fy, fw = mx + 28, my + 362, 420
    datos_ficha = [('SKU', prod['sku']), ('Garantía', '12 meses'),
                   ('Envío', 'gratis' if prod['precio'] >= 150000 else 'a calcular')]
    for i, (k, v) in enumerate(datos_ficha):
        bx = fx + i * (fw + 8) / 3
        bw = (fw - 16) / 3
        m += rect(bx, fy, bw, 68, 14, fill=FONDO, stroke=BORDE, sw=1)
        m += texto(bx + bw / 2, fy + 27, k.upper(), 10, 700, TINTA_3, anchor='middle', ls=1.1)
        m += texto(bx + bw / 2, fy + 49, v, 14, 700, TINTA, anchor='middle')

    m += rect(mx + mw - 66, my + 22, 40, 40, 13, fill=SUPERFICIE_2, stroke=BORDE, sw=1)
    m += icono('cerrar', mx + mw - 57, my + 31, 22, TINTA, 2)

    cx, cw = mx + 476, 476
    meta = f"{prod['marca']}  ·  {CATEGORIAS_NOMBRE[prod['categoria']]}"
    m += texto(cx, my + 62, meta.upper(), 11, 700, TINTA_3, ls=1.4)
    for i, ln in enumerate(envolver(prod['nombre'], 33, 700, cw - 60, 2)):
        m += texto(cx, my + 104 + i * 40, ln, 33, 700, TINTA, familia=DISPLAY)
    ex = cx + ancho_estrellas(14) + 10
    m += estrellas(cx, my + 178, 14)
    m += texto(ex, my + 178, f"{prod['rating']:.1f}", 14, 700, TINTA)
    m += texto(ex + ancho(f"{prod['rating']:.1f}", 14, 700) + 8, my + 178,
               f"({numero(prod['resenias'])} reseñas)", 13, 400, TINTA_3)

    m += texto(cx, my + 224, pesos(prod['precio']), 36, 700, TINTA)
    dx = cx
    if prod.get('precioAnterior'):
        m += texto(dx, my + 250, pesos(prod['precioAnterior']), 14, 400, TINTA_3,
                   tachado=True)
        dx += ancho(pesos(prod['precioAnterior']), 14) + 12
        p, wdt = pill(dx, my + 236, f"{descuento(prod['precio'], prod['precioAnterior'])} % OFF",
                      11.5, 700, ACENTO_SUAVE, ACENTO, 22, 9)
        m += p
    m += texto(cx, my + 274, f"12 cuotas de {cuota(prod['precio'])} sin interés",
               14, 500, TINTA_2)

    st, sc = texto_stock(prod)
    m += circ(cx + 5, my + 300, 4.5, fill=sc)
    m += texto(cx + 18, my + 305, st, 14, 600, sc)

    for i, ln in enumerate(envolver(prod['descripcion'], 14.5, 400, cw - 20, 3)):
        m += texto(cx, my + 336 + i * 22, ln, 14.5, 400, TINTA_2)

    hx = cx
    for d in prod['destacados']:
        p, wdt = pill(hx, my + 400, d, 12.5, 600, PROFUNDO_SUAVE, PROFUNDO, 30, 14)
        m += p
        hx += wdt + 8

    m += boton(cx, my + 452, 292, 54, 'Agregar al carrito', 'primario', 16)
    m += boton(cx + 304, my + 452, 172, 54, 'Seguir viendo', 'fantasma', 15)
    out += g('Componente / Modal de producto', m)
    return out


def frame_catalogo_mobile(datos, pfx='cm'):
    prods = datos['productos'][:4]
    out = rect(0, 0, W_M, H_M, fill=FONDO, nombre='Fondo')
    out += anuncio(W_M, 26, 10)
    out += header_mobile()

    out += g('Componente / Campo de búsqueda — Mobile',
             buscador(16, 94, 358, 44, 'Buscar productos…', 14))

    enc = texto(16, 176, 'EL CATÁLOGO', 10.5, 700, ACENTO, ls=1.5)
    enc += texto(16, 206, 'Lo que tenemos hoy', 26, 700, TINTA, familia=DISPLAY)
    out += g('Encabezado del catálogo', enc)

    fila = boton(16, 224, 112, 42, 'Filtros', 'fantasma', 14, icono_izq='filtros')
    fila += texto(374, 250, '16 productos', 12.5, 500, TINTA_3, anchor='end')
    out += g('Barra de filtros mobile', fila)

    grilla = ''
    for i, p in enumerate(prods):
        grilla += tarjeta_mobile(f'{pfx}-card-{i}', 16 + (i % 2) * 187,
                                 282 + (i // 2) * 314, p)
    out += g('Grilla de productos mobile', grilla)
    return out


def frame_filtros_mobile(datos, pfx='fm'):
    cats = ['Todas', 'Tecnología', 'Audio', 'Hogar', 'Oficina', 'Deportes']
    out = frame_catalogo_mobile(datos, pfx)
    out += rect(0, 0, W_M, H_M, fill=TINTA, opacity=0.55, nombre='Scrim')

    sy = 248
    d = (f'M0 {H_M} V{sy + 28} a28 28 0 0 1 28 -28 h{W_M - 56} '
         f'a28 28 0 0 1 28 28 V{H_M} Z')
    s = path(d, fill=SUPERFICIE)
    s += rect(W_M / 2 - 20, sy + 14, 40, 5, 2.5, fill=BORDE_FUERTE)
    s += texto(20, sy + 60, 'Filtros', 24, 700, TINTA, familia=DISPLAY)
    s += rect(W_M - 60, sy + 36, 36, 36, 12, fill=FONDO, stroke=BORDE, sw=1)
    s += icono('cerrar', W_M - 52, sy + 44, 20, TINTA, 2)
    s += linea(20, sy + 82, W_M - 20, sy + 82, BORDE)

    s += rotulo(20, sy + 114, 'Categoría')
    cx, cy = 20, sy + 126
    chips = ''
    for i, c in enumerate(cats):
        wdt = ancho(c, 13, 600) + 30
        if cx + wdt > W_M - 20:
            cx, cy = 20, cy + 42
        sel = i == 0
        p, _ = pill(cx, cy, c, 13, 600 if sel else 500,
                    PROFUNDO if sel else SUPERFICIE,
                    TINTA_INV if sel else TINTA_2, 34, 15,
                    borde=None if sel else BORDE_FUERTE)
        chips += p
        cx += wdt + 8
    s += g('Componente / Chip de filtro — Categorías', chips)

    py = sy + 236
    s += rotulo(20, py, 'Precio máximo')
    sl = rect(20, py + 14, 350, 6, 3, fill=PISTA)
    sl += rect(20, py + 14, 350 * 0.87, 6, 3, fill=ACENTO)
    sl += circ(20 + 350 * 0.87, py + 17, 12, fill=SUPERFICIE_2, stroke=ACENTO, sw=3)
    sl += texto(20, py + 54, 'Hasta', 13, 400, TINTA_2)
    sl += texto(60, py + 54, pesos(PRECIO_MAX), 14, 700, TINTA)
    sl += texto(370, py + 54, f'mín. {pesos(PRECIO_MIN)}', 11.5, 400, TINTA_3, anchor='end')
    s += g('Control de precio máximo', sl)

    dy = sy + 326
    s += rotulo(20, dy, 'Disponibilidad')
    sw_ = texto(20, dy + 34, 'Sólo con stock', 14.5, 500, TINTA)
    sw_ += interruptor(318, dy + 14, 52, 30, encendido=False)
    s += g('Componente / Control de stock', sw_)

    oy = sy + 406
    s += rotulo(20, oy, 'Ordenar por')
    s += g('Control de ordenamiento', selector(20, oy + 12, 350, 48, 'Relevancia'))

    s += linea(0, sy + 492, W_M, sy + 492, BORDE)
    s += boton(20, sy + 512, 160, 52, 'Limpiar filtros', 'fantasma', 14.5)
    s += boton(192, sy + 512, 178, 52, 'Aplicar filtros', 'primario', 14.5)
    out += g('Panel de filtros — Bottom sheet', s)
    return out


# --------------------------------------------------------------------------- #
# Ensamblado                                                                   #
# --------------------------------------------------------------------------- #

def _unicos(svg):
    """Casi todos los ids son nombres de capa y nadie los referencia: si el
    lienzo repite un componente, se numeran para que el XML siga siendo valido.
    Los que si estan referenciados (clipPath) quedan intactos."""
    referenciados = set(re.findall(r'url\(#([^)]+)\)', svg))
    vistos = {}

    def sub(m):
        nombre = m.group(1)
        if nombre in referenciados:
            return m.group(0)
        vistos[nombre] = vistos.get(nombre, 0) + 1
        n = vistos[nombre]
        return f'id="{nombre}"' if n == 1 else f'id="{nombre} ({n})"'

    return re.sub(r'id="([^"]*)"', sub, svg)


def documento(w, h, cuerpo, titulo):
    cuerpo = _unicos(cuerpo)
    defs = '<defs>' + ''.join(_CLIPS) + '</defs>' if _CLIPS else ''
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" '
            f'viewBox="0 0 {w} {h}" fill="none">'
            f'<title>{esc(titulo)}</title>{defs}{cuerpo}</svg>')


FRAMES = [
    ('01-catalogo-desktop',  'Catálogo Desktop',            W_D, H_D, frame_catalogo_desktop, 'd'),
    ('02-detalle-desktop',   'Detalle de producto Desktop', W_D, H_D, frame_detalle_desktop,  'm'),
    ('03-catalogo-mobile',   'Catálogo Mobile',             W_M, H_M, frame_catalogo_mobile,  'cm'),
    ('04-filtros-mobile',    'Filtros Mobile',              W_M, H_M, frame_filtros_mobile,   'fm'),
]


def flujo(datos):
    """Lienzo unico con los cuatro frames y las conexiones del prototipo."""
    esc_d, esc_m = 0.45, 0.45
    wd, hd = W_D * esc_d, H_D * esc_d
    wm, hm = W_M * esc_m, H_M * esc_m
    mx, top = 64, 210
    gap = 66
    xs = [mx]
    xs.append(xs[0] + wd + gap)
    xs.append(xs[1] + wd + gap)
    xs.append(xs[2] + wm + gap)
    ancho_total = xs[3] + wm + mx
    alto_total = top + hd + 92

    out = rect(0, 0, ancho_total, alto_total, fill=FONDO_ALT, nombre='Fondo')
    out += texto(mx, 74, 'TPO STORE · TRABAJO PRÁCTICO INTEGRADOR — DESARROLLO DE APLICACIONES II',
                 13, 700, ACENTO, ls=1.8)
    out += texto(mx, 122, 'Checkpoint 1 — Catálogo · flujo del prototipo',
                 38, 700, TINTA, familia=DISPLAY)
    out += texto(mx, 152, 'Punto de inicio: Catálogo Desktop. Sólo se prototipa el componente Catálogo.',
                 14, 500, TINTA_2)

    constructores = [
        (frame_catalogo_desktop, 'f1', W_D, H_D, esc_d, '1 · Catálogo Desktop', '1440 × 1024'),
        (frame_detalle_desktop,  'f2', W_D, H_D, esc_d, '2 · Detalle de producto', '1440 × 1024'),
        (frame_catalogo_mobile,  'f3', W_M, H_M, esc_m, '3 · Catálogo Mobile', '390 × 844'),
        (frame_filtros_mobile,   'f4', W_M, H_M, esc_m, '4 · Filtros Mobile', '390 × 844'),
    ]
    for i, (fn, pfx, fw, fh, s, rot, med) in enumerate(constructores):
        x = xs[i]
        w, h = fw * s, fh * s
        out += rect(x - 6, top - 6, w + 12, h + 12, 14, fill=SUPERFICIE, stroke=BORDE, sw=1)
        cid = _clip(f'flujo-{pfx}', x, top, w, h, 8)
        out += (f'<g clip-path="url(#{cid})">'
                f'<g transform="translate({x:g} {top:g}) scale({s:g})">'
                f'{fn(datos, pfx)}</g></g>')
        out += texto(x, top - 44, rot, 15, 700, TINTA)
        out += texto(x, top - 24, med, 12, 500, TINTA_3)

    def conector(x1, x2, y, arriba, abajo):
        mid = (x1 + x2) / 2
        c = path(f'M{x1} {y} H{x2 - 12}', stroke=ACENTO, sw=2)
        c += path(f'M{x2 - 18} {y - 6} l6 6 l-6 6', stroke=ACENTO, sw=2)
        c += texto(mid, y - 12, arriba, 12.5, 700, ACENTO, anchor='middle')
        if abajo:
            c += path(f'M{x2 - 12} {y + 26} H{x1 + 12}', stroke=TINTA_3, sw=1.6)
            c += path(f'M{x1 + 18} {y + 20} l-6 6 l6 6', stroke=TINTA_3, sw=1.6)
            c += texto(mid, y + 48, abajo, 12, 500, TINTA_3, anchor='middle')
        return c

    ymid = top + hd / 2 - 20
    out += conector(xs[0] + wd + 12, xs[1] - 12, ymid, 'Ver detalle', 'Cerrar (×)')
    out += conector(xs[2] + wm + 12, xs[3] - 12, top + hm / 2 - 20, 'Filtros',
                    'Aplicar filtros')
    out += texto(xs[1] + wd + gap / 2, top + hd + 46,
                 'Responsive del mismo componente', 12.5, 500, TINTA_3, anchor='middle')
    return ancho_total, alto_total, out


def main():
    global _CLIPS
    with open(DATOS, encoding='utf-8') as f:
        datos = json.load(f)

    generados = []
    for slug, titulo, w, h, fn, pfx in FRAMES:
        _CLIPS = []
        cuerpo = g(f'Frame / {titulo}', fn(datos, pfx))
        svg = documento(w, h, cuerpo, f'Checkpoint 1 — Catálogo · {titulo}')
        ruta = os.path.join(RAIZ, f'{slug}.svg')
        with open(ruta, 'w', encoding='utf-8') as f:
            f.write(svg)
        generados.append((ruta, w, h))
        print(f'  {slug}.svg  {w}×{h}')

    _CLIPS = []
    fw, fh, cuerpo = flujo(datos)
    svg = documento(fw, fh, g('Flujo del prototipo', cuerpo),
                    'Checkpoint 1 — Catálogo · flujo del prototipo')
    ruta = os.path.join(RAIZ, '05-flujo.svg')
    with open(ruta, 'w', encoding='utf-8') as f:
        f.write(svg)
    generados.append((ruta, fw, fh))
    print(f'  05-flujo.svg  {fw:g}×{fh:g}')
    return generados


if __name__ == '__main__':
    main()

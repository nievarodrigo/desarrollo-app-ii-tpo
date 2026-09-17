#!/usr/bin/env python3
"""
Imprime las carpetas del cliente y las del servidor UNA AL LADO DE LA OTRA.

Es lo que se muestra en el bloque de apertura: "es la misma forma de los dos
lados del cable". En una terminal a 18 pt se lee desde el fondo del aula y no
tiene el ruido visual de la barra lateral de un editor.

    ./scripts/capas.py

Va en Python y no en bash porque `printf '%-*s'` de bash alinea por BYTES, y
los caracteres de árbol (├ │ ─) ocupan 3 bytes cada uno en UTF-8: las columnas
quedaban corridas.
"""
import pathlib
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
FRONT = RAIZ / 'src/main/webapp/assets/js/componentes'
BACK = RAIZ / 'src/main/java/ar/edu/desarrolloappii/ecommerce/catalogo'
COLUMNA = 46

CAPA = {
    'presentacion': 'presentación',
    'controller': 'presentación',
    'negocio': 'negocio',
    'service': 'negocio',
    'datos': 'datos',
    'repository': 'datos',
}

NEGRITA, APAGADO, FIN = '\033[1m', '\033[90m', '\033[0m'


def arbol(raiz: pathlib.Path, nivel=0, prefijo=''):
    """Sólo directorios, ordenados, con las guías del árbol."""
    if not raiz.is_dir():
        return []
    hijos = sorted(h for h in raiz.iterdir() if h.is_dir())
    lineas = []
    for i, h in enumerate(hijos):
        ultimo = i == len(hijos) - 1
        lineas.append((f"{prefijo}{'└── ' if ultimo else '├── '}{h.name}", h.name))
        lineas += arbol(h, nivel + 1, prefijo + ('    ' if ultimo else '│   '))
    return lineas


def con_capa(linea, nombre):
    capa = CAPA.get(nombre)
    return f'{linea}  {APAGADO}← {capa}{FIN}' if capa else linea


def ancho_visible(texto):
    """Largo sin contar los códigos de color."""
    limpio, i = '', 0
    while i < len(texto):
        if texto[i] == '\033':
            i = texto.find('m', i) + 1
        else:
            limpio += texto[i]
            i += 1
    return len(limpio)


def main():
    izq = [con_capa(l, n) for l, n in arbol(FRONT)]
    der = [con_capa(l, n) for l, n in arbol(BACK)]

    if not izq or not der:
        print(f'No encontré las carpetas.\n  {FRONT}\n  {BACK}', file=sys.stderr)
        return 1

    def fila(a, b):
        relleno = max(0, COLUMNA - ancho_visible(a))
        print(f'{a}{" " * relleno}{b}')

    print()
    fila(f'{NEGRITA}CLIENTE  (JavaScript){FIN}', f'{NEGRITA}SERVIDOR  (Jakarta EE 10){FIN}')
    fila(f'{APAGADO}assets/js/componentes/{FIN}', f'{APAGADO}…/ecommerce/catalogo/{FIN}')
    fila('─' * 30, '─' * 30)

    for i in range(max(len(izq), len(der))):
        fila(izq[i] if i < len(izq) else '', der[i] if i < len(der) else '')

    print(f"""
  Misma forma de los dos lados del cable.

  Y no es decoración: permite decir con precisión qué pieza del cliente
  se borra cuando entre su equivalente del servidor.
""")
    return 0


if __name__ == '__main__':
    sys.exit(main())

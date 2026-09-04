#!/usr/bin/env python3
"""Renderiza cada SVG a PNG (2x) con Chromium headless, cargando Archivo y Fraunces."""

import glob
import os
import re
import shutil
import subprocess
import sys
import tempfile

RAIZ = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(RAIZ, 'exports')
ESCALA = 2

CANDIDATOS = [
    os.path.expanduser('~/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome'),
    os.path.expanduser('~/.cache/puppeteer/chrome/linux-151.0.7922.71/chrome-linux64/chrome'),
    shutil.which('chromium') or '', shutil.which('google-chrome') or '',
]
CHROME = next((c for c in CANDIDATOS if c and os.path.exists(c)), None)
if not CHROME:
    sys.exit('No encontre un binario de Chromium para renderizar.')

PLANTILLA = """<!doctype html><html><head><meta charset="utf-8">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Archivo:wght@400..700&family=Fraunces:opsz,wght,SOFT,WONK@9..144,400..800,0..100,0..1&display=block">
<style>html,body{{margin:0;padding:0;background:transparent}}svg{{display:block}}</style>
</head><body>{svg}</body></html>"""

os.makedirs(SALIDA, exist_ok=True)
tmp = tempfile.mkdtemp(prefix='tpo-figma-')

for ruta in sorted(glob.glob(os.path.join(RAIZ, '*.svg'))):
    svg = open(ruta, encoding='utf-8').read()
    m = re.search(r'width="([\d.]+)" height="([\d.]+)"', svg)
    w, h = int(round(float(m.group(1)))), int(round(float(m.group(2))))
    html = os.path.join(tmp, os.path.basename(ruta) + '.html')
    open(html, 'w', encoding='utf-8').write(PLANTILLA.format(svg=svg))
    png = os.path.join(SALIDA, os.path.basename(ruta).replace('.svg', '.png'))
    cmd = [CHROME, '--headless=new', '--disable-gpu', '--no-sandbox',
           '--hide-scrollbars', '--force-color-profile=srgb',
           f'--force-device-scale-factor={ESCALA}',
           '--virtual-time-budget=8000',
           f'--window-size={w},{h}', f'--screenshot={png}', f'file://{html}']
    subprocess.run(cmd, check=True, capture_output=True)
    print(f'  {os.path.basename(png)}  {w * ESCALA}×{h * ESCALA}')

shutil.rmtree(tmp, ignore_errors=True)

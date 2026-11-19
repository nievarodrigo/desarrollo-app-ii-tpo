#!/usr/bin/env python3
"""Convierte los .md de la entrega a PDF con el estilo de la marca (sin dependencias).

    python3 scripts/docs/md_a_pdf.py          # genera entrega-3/pdf/*.pdf

Soporta lo que usan nuestros documentos: títulos, párrafos, listas, tablas, bloques de
código, citas, separadores, imágenes, negrita, cursiva, código en línea y enlaces.
"""
import html
import re
import subprocess
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SALIDA = RAIZ / "entrega-3" / "pdf"
ESTILO = RAIZ / "entrega-3" / "presentacion" / "estilo.css"

DOCUMENTOS = [
    ("entrega-3/RESUMEN.md", "01_Informe_Checkpoint3_GOMARKET", "Informe de entrega"),
    ("entrega-3/PLAN_DE_PRUEBAS.md", "02_Plan_de_Pruebas_GOMARKET", "Plan de pruebas"),
    ("entrega-3/MATRIZ_TRAZABILIDAD.md", "03_Matriz_de_Trazabilidad_GOMARKET", "Matriz de trazabilidad"),
    ("entrega-3/MANUAL_DE_USUARIO.md", "04_Manual_de_Usuario_GOMARKET", "Manual de usuario"),
    ("entrega-3/DECLARACION_USO_IA.md", "05_Declaracion_Uso_IA_GOMARKET", "Declaración de uso de IA"),
    ("docs/ia/FICHA_IA.md", "06_Ficha_IA_GOMARKET", "Ficha de la feature de IA"),
    (["docs/adr/adr-011.md", "docs/adr/adr-012.md", "docs/adr/adr-013.md"], "07_ADR_011-013_GOMARKET", "ADR del checkpoint"),
]


def en_linea(texto: str, base: Path) -> str:
    t = html.escape(texto, quote=False)
    codigos = []
    t = re.sub(r"`([^`]+)`", lambda m: codigos.append(m.group(1)) or f"\x00{len(codigos) - 1}\x00", t)

    def imagen(m):
        src = m.group(2)
        if not re.match(r"https?://", src):
            src = (base / src).resolve().as_uri()
        return f'<img src="{src}" alt="{m.group(1)}" style="max-width:100%;border:1px solid #d9cfbf;border-radius:6px;margin:2mm 0">'

    t = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)", imagen, t)
    t = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', t)
    t = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", t)
    t = re.sub(r"(?<![*\w])\*([^*\n]+)\*(?!\*)", r"<i>\1</i>", t)
    return re.sub(r"\x00(\d+)\x00", lambda m: f'<code>{codigos[int(m.group(1))]}</code>', t)


def convertir(md: str, base: Path) -> str:
    salida, lineas, i = [], md.splitlines(), 0
    while i < len(lineas):
        linea = lineas[i]
        if linea.startswith("```"):
            bloque = []
            i += 1
            while i < len(lineas) and not lineas[i].startswith("```"):
                bloque.append(lineas[i]); i += 1
            salida.append("<pre>" + html.escape("\n".join(bloque)) + "</pre>")
        elif re.match(r"^#{1,4} ", linea):
            nivel = len(linea.split(" ")[0])
            salida.append(f"<h{min(nivel + 1, 4) if nivel > 1 else 1}>{en_linea(linea[nivel + 1:], base)}</h{min(nivel + 1, 4) if nivel > 1 else 1}>")
        elif linea.startswith("|"):
            filas = []
            while i < len(lineas) and lineas[i].startswith("|"):
                filas.append(lineas[i]); i += 1
            celdas = [[c.strip() for c in f.strip().strip("|").split("|")] for f in filas if not re.match(r"^\|[\s:|-]+\|$", f)]
            t = ["<table>", "<tr>" + "".join(f"<th>{en_linea(c, base)}</th>" for c in celdas[0]) + "</tr>"]
            t += ["<tr>" + "".join(f"<td>{en_linea(c, base)}</td>" for c in fila) + "</tr>" for fila in celdas[1:]]
            salida.append("\n".join(t) + "</table>")
            continue
        elif re.match(r"^\s*([-*]|\d+\.) ", linea):
            ordenada = bool(re.match(r"^\s*\d+\.", linea))
            items = []
            while i < len(lineas) and re.match(r"^\s*([-*]|\d+\.) ", lineas[i]):
                items.append(re.sub(r"^\s*([-*]|\d+\.) ", "", lineas[i])); i += 1
            tag = "ol" if ordenada else "ul"
            salida.append(f"<{tag}>" + "".join(f"<li>{en_linea(x, base)}</li>" for x in items) + f"</{tag}>")
            continue
        elif linea.startswith(">"):
            cita = []
            while i < len(lineas) and lineas[i].startswith(">"):
                cita.append(lineas[i].lstrip("> ")); i += 1
            salida.append(f'<div class="caja mostaza">{en_linea(" ".join(cita), base)}</div>')
            continue
        elif linea.strip() == "---":
            salida.append('<hr style="border:none;border-top:1px solid #d9cfbf;margin:5mm 0">')
        elif linea.strip():
            parrafo = [linea]
            while i + 1 < len(lineas) and lineas[i + 1].strip() and not re.match(r"^(#|\||```|>|\s*([-*]|\d+\.) |---)", lineas[i + 1]):
                i += 1; parrafo.append(lineas[i])
            salida.append(f"<p>{en_linea(' '.join(parrafo), base)}</p>")
        i += 1
    return "\n".join(salida)


def main():
    SALIDA.mkdir(parents=True, exist_ok=True)
    for fuentes, nombre, titulo in DOCUMENTOS:
        fuentes = fuentes if isinstance(fuentes, list) else [fuentes]
        cuerpo = "\n".join(convertir((RAIZ / f).read_text(encoding="utf-8"), (RAIZ / f).parent) for f in fuentes)
        pagina = f"""<!doctype html><html lang="es"><head><meta charset="utf-8"><title>{titulo} · GOMARKET</title>
<link rel="stylesheet" href="{ESTILO.as_uri()}"><style>@page:first{{margin:18mm 16mm}} h1{{font-size:20pt;color:#1e332c;border-bottom:2px solid #bd3a1e;padding-bottom:2mm}}</style></head>
<body><p class="ojo">GOMARKET · Grupo 5 · Desarrollo de Aplicaciones II · 3.er checkpoint · 20/11/2026</p>{cuerpo}</body></html>"""
        archivo_html = SALIDA / f"{nombre}.html"
        archivo_html.write_text(pagina, encoding="utf-8")
        subprocess.run(["google-chrome", "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                        "--virtual-time-budget=8000", f"--print-to-pdf={SALIDA / (nombre + '.pdf')}", archivo_html.as_uri()],
                       check=True, stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL)
        archivo_html.unlink()
        print("✔", nombre + ".pdf")


if __name__ == "__main__":
    main()

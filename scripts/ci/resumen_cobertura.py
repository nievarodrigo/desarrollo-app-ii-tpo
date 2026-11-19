#!/usr/bin/env python3
"""Resume target/site/jacoco/jacoco.csv en una tabla Markdown por paquete (para el resumen del CI)."""
import csv
import sys
from collections import defaultdict

ruta = sys.argv[1] if len(sys.argv) > 1 else "target/site/jacoco/jacoco.csv"
por_paquete = defaultdict(lambda: [0, 0, 0, 0])  # líneas cubiertas, perdidas, ramas cubiertas, perdidas
with open(ruta, newline="", encoding="utf-8") as archivo:
    for fila in csv.DictReader(archivo):
        p = por_paquete[fila["PACKAGE"].replace("ar.edu.desarrolloappii.ecommerce.", "")]
        p[0] += int(fila["LINE_COVERED"]); p[1] += int(fila["LINE_MISSED"])
        p[2] += int(fila["BRANCH_COVERED"]); p[3] += int(fila["BRANCH_MISSED"])

def pct(cubiertas, perdidas):
    total = cubiertas + perdidas
    return f"{100 * cubiertas / total:.0f}%" if total else "—"

total = [sum(v[i] for v in por_paquete.values()) for i in range(4)]
print("## Cobertura de tests (JaCoCo, parcial)\n")
print(f"**Total: {pct(total[0], total[1])} de líneas · {pct(total[2], total[3])} de ramas**\n")
print("| Paquete | Líneas | Ramas |")
print("|---|---:|---:|")
for nombre, (lc, lm, bc, bm) in sorted(por_paquete.items()):
    print(f"| `{nombre}` | {pct(lc, lm)} | {pct(bc, bm)} |")

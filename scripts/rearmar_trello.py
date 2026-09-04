#!/usr/bin/env python3
"""Rearma el tablero grande como un tablero operativo y tranquilo.

Por defecto sólo muestra qué cambiaría. Usar --apply para archivar las tarjetas
viejas del plan detallado, cargar el plan simple y cerrar listas antiguas vacías.
No elimina contenido: las tarjetas y listas quedan archivadas en Trello.
"""

from __future__ import annotations

import argparse
import re

import cargar_trello as trello

OLD_ID = re.compile(r"^\[(INI|WEB|E1|E2|E3|E4|FIN|EXTRA)-")
LEGACY_LISTS = {
    "Información y decisiones",
    "Backlog priorizado",
    "Próxima entrega",
    "Bloqueado",
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="aplica los cambios")
    args = parser.parse_args()

    board = trello.api("GET", f"/boards/{trello.BOARD}", {"fields": "name,url"})
    lists = trello.api("GET", f"/boards/{board['id']}/lists", {
        "filter": "open",
        "fields": "name",
    })
    cards = trello.api("GET", f"/boards/{board['id']}/cards", {
        "filter": "open",
        "fields": "name,idList",
    })

    desired = {task["title"] for task in trello.parse_plan()}
    obsolete = [
        card for card in cards
        if OLD_ID.match(card["name"]) and card["name"] not in desired
    ]
    existing_desired = [card for card in cards if card["name"] in desired]

    print(f"Tablero: {board['name']} — {board['url']}")
    print(f"Tarjetas operativas deseadas: {len(desired)}")
    print(f"Tarjetas nuevas por crear: {len(desired) - len(existing_desired)}")
    print(f"Tarjetas del plan detallado por archivar: {len(obsolete)}")
    print("Modo:", "APLICAR" if args.apply else "VISTA PREVIA")
    for card in obsolete:
        print("  archivar:", card["name"])

    if not args.apply:
        print("\nNo se cambió nada. Repetir con --apply para confirmar.")
        return

    for card in obsolete:
        trello.api("PUT", f"/cards/{card['id']}", {"closed": "true"})

    trello.main()

    remaining = trello.api("GET", f"/boards/{board['id']}/cards", {
        "filter": "open",
        "fields": "name,idList",
    })
    occupied = {card["idList"] for card in remaining}
    for board_list in lists:
        if board_list["name"] in LEGACY_LISTS and board_list["id"] not in occupied:
            trello.api("PUT", f"/lists/{board_list['id']}/closed", {"value": "true"})
            print("Lista antigua archivada:", board_list["name"])

    print("\nTablero simplificado. El plan anterior sigue disponible en elementos archivados.")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Carga idempotente del PLAN_TRELLO.md en un tablero Trello existente."""

from __future__ import annotations

import json
import os
import re
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter
from datetime import datetime, timezone, timedelta
from pathlib import Path

API = "https://api.trello.com/1"
KEY = os.environ["TRELLO_API_KEY"]
TOKEN = os.environ["TRELLO_TOKEN"]
BOARD = os.environ.get("TRELLO_BOARD", "gbrISl9W")
ROOT = Path(__file__).resolve().parents[1]

LISTS = [
    "Decisiones",
    "Esta semana",
    "En curso",
    "Revisión cruzada",
    "Hecho",
    "Después",
]

LABELS = {
    "Bloqueante": "black",
    "Arquitectura": "blue",
    "Backend": "sky",
    "Frontend": "pink",
    "Datos": "purple",
    "Seguridad": "red",
    "Integración": "orange",
    "Mensajería": "lime",
    "Documentación": "yellow",
    "Testing/DevOps": "green",
}

CHECKLIST = [
    "Funciona y puede demostrarse",
    "Evidencia enlazada",
    "Revisión cruzada aprobada",
    "Responsable y revisor pueden explicarlo",
]


def api(method: str, path: str, params: dict | None = None):
    data = urllib.parse.urlencode(params or {}, doseq=True).encode() if method != "GET" else None
    url = API + path
    if method == "GET" and params:
        url += "?" + urllib.parse.urlencode(params, doseq=True)
    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={
            "Authorization": f'OAuth oauth_consumer_key="{KEY}", oauth_token="{TOKEN}"',
            "Accept": "application/json",
            "Content-Type": "application/x-www-form-urlencoded",
        },
    )
    for attempt in range(5):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                payload = response.read()
                time.sleep(0.12)
                return json.loads(payload) if payload else None
        except urllib.error.HTTPError as exc:
            if (exc.code == 429 or 500 <= exc.code < 600) and attempt < 4:
                wait = float(exc.headers.get("Retry-After", str(2 ** attempt)))
                time.sleep(wait)
                continue
            detail = exc.read().decode(errors="replace")
            raise RuntimeError(f"Trello {method} {path}: HTTP {exc.code}: {detail}") from exc


def normalized(value: str) -> str:
    value = unicodedata.normalize("NFD", value)
    return "".join(ch for ch in value if unicodedata.category(ch) != "Mn").lower().strip()


def parse_plan():
    section = ""
    result = []
    for line in (ROOT / "PLAN_TRELLO.md").read_text(encoding="utf-8").splitlines():
        if line.startswith("## "):
            section = line[3:].strip()
        if not re.match(r"^\| `\[", line):
            continue
        parts = [part.strip() for part in line.strip("|").split("|")]
        title = parts[0].strip("`")
        result.append({
            "title": title,
            "owner": parts[1],
            "reviewer": parts[2],
            "due": parts[3],
            "acceptance": parts[4],
            "section": section,
        })
    return result


def destination(section: str) -> str:
    if section.startswith("ESTA SEMANA"):
        return "Esta semana"
    return "Después"


def due_iso(value: str) -> str | None:
    match = re.fullmatch(r"(\d{2})/(\d{2})", value)
    if not match:
        return None
    day, month = map(int, match.groups())
    tz = timezone(timedelta(hours=-3))
    return datetime(2026, month, day, 13, 30, tzinfo=tz).isoformat()


def classify(title: str) -> list[str]:
    text = normalized(title)
    labels = set()
    rules = {
        "Bloqueante": ("aprobar", "confirmar fecha", "validar trello"),
        "Arquitectura": ("arquitectura", "stack", "stateless", "stateful", "facade", "strategy", "adapter", "checklist transversal"),
        "Backend": ("catalogo", "carrito", "usuarios", "pedidos", "checkout", "resource", "autenticacion"),
        "Frontend": ("landing", "responsive", "figma", "ui,", "pantallas", "prototipo"),
        "Datos": ("jpa", "repository", "persistencia", "transaccional"),
        "Seguridad": ("seguridad", "autenticacion", "rol"),
        "Integración": ("soap", "rest", "integrar", "soa"),
        "Mensajería": ("cola", "topico", "mensajeria", "artemis", "asincronico"),
        "Documentación": ("documentar", "dibujar", "justificar", "redactar", "documento", "historias", "manual", "auditar", "defensa", "matriz"),
        "Testing/DevOps": ("maven", "wildfly", "repositorio", "pruebas", "deploy", "demo", "broker", "git"),
    }
    for label, words in rules.items():
        if any(word in text for word in words):
            labels.add(label)
    return sorted(labels or {"Backend"})


def member_ids(value: str, members: dict[str, str]) -> list[str]:
    if normalized(value) == "los cinco":
        return list(members.values())
    first_name = normalized(value).split()[0]
    return [member_id for name, member_id in members.items() if normalized(name).split()[0] == first_name]


def ensure_checklist(card_id: str):
    checklists = api("GET", f"/cards/{card_id}/checklists", {
        "checkItems": "all",
        "fields": "name",
        "checkItem_fields": "name",
    })
    checklist = next((item for item in checklists if item["name"] == "Definition of Done"), None)
    if checklist is None:
        checklist = api("POST", f"/cards/{card_id}/checklists", {"name": "Definition of Done", "pos": "bottom"})
        existing_items = set()
    else:
        existing_items = {item["name"] for item in checklist.get("checkItems", [])}
    for item in CHECKLIST:
        if item not in existing_items:
            api("POST", f"/checklists/{checklist['id']}/checkItems", {"name": item, "pos": "bottom", "checked": "false"})


def main():
    board = api("GET", f"/boards/{BOARD}", {"fields": "name,url"})
    board_id = board["id"]

    existing_lists = api("GET", f"/boards/{board_id}/lists", {"filter": "open", "fields": "name"})
    list_ids = {item["name"]: item["id"] for item in existing_lists}
    for position, name in enumerate(LISTS, 1):
        if name not in list_ids:
            created = api("POST", "/lists", {"name": name, "idBoard": board_id, "pos": position * 1000})
            list_ids[name] = created["id"]

    existing_labels = api("GET", f"/boards/{board_id}/labels", {"limit": 100, "fields": "name,color"})
    label_ids = {item["name"]: item["id"] for item in existing_labels if item.get("name")}
    for name, color in LABELS.items():
        if name not in label_ids:
            created = api("POST", f"/boards/{board_id}/labels", {"name": name, "color": color})
            label_ids[name] = created["id"]

    board_members = api("GET", f"/boards/{board_id}/members", {"fields": "fullName,username"})
    members = {item["fullName"]: item["id"] for item in board_members}
    existing_cards = api("GET", f"/boards/{board_id}/cards", {"filter": "open", "fields": "name"})
    existing_by_name = {item["name"]: item for item in existing_cards}

    created_count = 0
    skipped_count = 0
    missing_people = Counter()
    for task in parse_plan():
        if task["title"] in existing_by_name:
            ensure_checklist(existing_by_name[task["title"]]["id"])
            skipped_count += 1
            continue
        ids = set(member_ids(task["owner"], members) + member_ids(task["reviewer"], members))
        if not member_ids(task["owner"], members) and normalized(task["owner"]) != "los cinco":
            missing_people[task["owner"]] += 1
        if not member_ids(task["reviewer"], members) and normalized(task["reviewer"]) != "los cinco":
            missing_people[task["reviewer"]] += 1
        labels = classify(task["title"])
        description = (
            f"## Resultado esperado\n{task['acceptance']}\n\n"
            f"## Asignación\n- **Responsable:** {task['owner']}\n- **Revisor:** {task['reviewer']}\n\n"
            f"## Entrega\n{task['section']}\n\n"
            "## Seguimiento\nAdjuntar enlaces a commits/PR, pruebas y documentación antes de mover a Hecho."
        )
        params = {
            "idList": list_ids[destination(task["section"])],
            "name": task["title"],
            "desc": description,
            "idMembers": ",".join(sorted(ids)),
            "idLabels": ",".join(label_ids[label] for label in labels),
            "pos": "bottom",
        }
        due = due_iso(task["due"])
        if due:
            params["due"] = due
        card = api("POST", "/cards", params)
        ensure_checklist(card["id"])
        created_count += 1
        print(f"[{created_count:02d}] {task['title']}", flush=True)

    print(json.dumps({
        "board": board["name"],
        "url": board["url"],
        "cards_created": created_count,
        "cards_skipped": skipped_count,
        "members_found": list(members),
        "missing_assignments": dict(missing_people),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

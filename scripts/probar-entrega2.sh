#!/usr/bin/env bash
#
# Recorre el flujo de la 2.ª entrega contra un WildFly ya levantado:
# login JWT -> control de rol -> pedido -> mensajería (MDB) -> State -> 409.
#
#   ./scripts/probar-entrega2.sh                       # http://localhost:8080/ecommerce/api/v1
#   ./scripts/probar-entrega2.sh http://otro-host:8080/ecommerce/api/v1
#
# Cómo levantar el servidor sin sudo: ver .entrega2/RESUMEN.md o el README.

set -uo pipefail
B="${1:-http://localhost:8080/ecommerce/api}"
JSON='Content-Type: application/json'
campo() { python3 -c "import sys,json; print(json.load(sys.stdin)['$1'])"; }
paso()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
codigo() { curl -s -o /dev/null -w "%{http_code}" "$@"; }
esperar() { # esperar <descripcion> <esperado> <obtenido>
  if [ "$2" = "$3" ]; then printf '  \033[32mOK\033[0m   %s -> %s\n' "$1" "$3"
  else printf '  \033[31mFALLA\033[0m %s -> esperaba %s, dio %s\n' "$1" "$2" "$3"; FALLAS=1; fi
}
FALLAS=0

paso "1 · Seguridad"
esperar "pedido sin token" 401 "$(codigo -X POST "$B/pedidos" -H "$JSON" -d '{"items":[{"idProducto":1,"cantidad":1}]}')"
esperar "login con clave mala" 401 "$(codigo -X POST "$B/auth/login" -H "$JSON" -d '{"email":"cliente@gomarket.com.ar","clave":"mala"}')"
CT=$(curl -s -X POST "$B/auth/login" -H "$JSON" -d '{"email":"cliente@gomarket.com.ar","clave":"cliente123"}' | campo token)
AT=$(curl -s -X POST "$B/auth/login" -H "$JSON" -d '{"email":"admin@gomarket.com.ar","clave":"admin123"}' | campo token)
esperar "cliente lista usuarios (solo ADMIN)" 403 "$(codigo "$B/auth/usuarios" -H "Authorization: Bearer $CT")"
esperar "admin lista usuarios" 200 "$(codigo "$B/auth/usuarios" -H "Authorization: Bearer $AT")"
esperar "token adulterado" 401 "$(codigo "$B/auth/yo" -H "Authorization: Bearer ${CT%?}x")"

paso "2 · Pedido de punta a punta (REST -> JMS -> MDB)"
ID=$(curl -s -X POST "$B/pedidos" -H "Authorization: Bearer $CT" -H "$JSON" -d '{"items":[{"idProducto":1,"cantidad":1}]}' | campo id)
echo "  pedido creado: $ID"
sleep 1
ESTADO=$(curl -s "$B/pedidos/$ID" -H "Authorization: Bearer $CT" | campo estado)
esperar "el MDB lo paso de PENDIENTE a" PROCESADO "$ESTADO"
esperar "cliente intenta confirmar" 403 "$(codigo -X POST "$B/pedidos/$ID/confirmar" -H "Authorization: Bearer $CT")"
esperar "admin confirma" 200 "$(codigo -X POST "$B/pedidos/$ID/confirmar" -H "Authorization: Bearer $AT")"

paso "3 · Patron State: transicion invalida"
esperar "cancelar un pedido CONFIRMADO" 409 "$(codigo -X POST "$B/pedidos/$ID/cancelar" -H "Authorization: Bearer $CT")"
curl -s -X POST "$B/pedidos/$ID/cancelar" -H "Authorization: Bearer $CT"; echo
esperar "el pedido sigue CONFIRMADO" CONFIRMADO "$(curl -s "$B/pedidos/$ID" -H "Authorization: Bearer $CT" | campo estado)"

echo
[ "$FALLAS" = 0 ] && printf '\033[32mTodo OK\033[0m\n' || { printf '\033[31mHubo fallas\033[0m\n'; exit 1; }

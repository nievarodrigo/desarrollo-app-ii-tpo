#!/usr/bin/env bash
#
# Levanta todo lo que hace falta para la demo de la defensa.
#
#   ./scripts/demo.sh          front + navegador        (lo mínimo)
#   ./scripts/demo.sh --tests  ... y corre los 84 tests
#   ./scripts/demo.sh --todo   ... y además reconstruye el WAR y levanta WildFly
#
# Al final imprime el checklist de lo que hay que hacer a mano (agrandar
# fuentes, limpiar la sesión), que es justo lo que se olvida.

set -uo pipefail

PUERTO=4321
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEBAPP="$RAIZ/src/main/webapp"
WILDFLY="$(cd "$RAIZ/.." && pwd)/wildfly/wildfly-41-ee10"
LOG_FRONT="/tmp/gomarket-front.log"

verde()  { printf '\033[32m%s\033[0m\n' "$*"; }
rojo()   { printf '\033[31m%s\033[0m\n' "$*"; }
gris()   { printf '\033[90m%s\033[0m\n' "$*"; }
titulo() { printf '\n\033[1m%s\033[0m\n' "$*"; }

# ─────────────────────────────────────────────────────────── front
titulo "1 · Servidor del front"

# Si quedó algo del ensayo anterior, se baja. Se busca por PUERTO y no por
# nombre de proceso: un pkill por patrón puede matarte la propia terminal.
if pid=$(ss -ltnpH "sport = :$PUERTO" 2>/dev/null | grep -oP 'pid=\K[0-9]+' | head -1); then
  if [ -n "${pid:-}" ]; then
    gris "  ya había algo en el puerto $PUERTO (pid $pid), lo bajo"
    kill "$pid" 2>/dev/null
    sleep 1
  fi
fi

( cd "$WEBAPP" && exec python3 -m http.server "$PUERTO" ) >"$LOG_FRONT" 2>&1 &
FRONT_PID=$!

for _ in $(seq 20); do
  curl -fs -o /dev/null "http://localhost:$PUERTO/" && break
  sleep 0.25
done

if curl -fs -o /dev/null "http://localhost:$PUERTO/"; then
  verde "  OK  http://localhost:$PUERTO   (pid $FRONT_PID)"
else
  rojo  "  FALLÓ. Mirá $LOG_FRONT"
  exit 1
fi

# ─────────────────────────────────────────────────────────── tests
if [[ " $* " == *" --tests "* || " $* " == *" --todo "* ]]; then
  titulo "2 · Tests del front"
  ( cd "$RAIZ" && node --test 'src/test/js/**/*.test.mjs' 2>&1 | tail -6 )
fi

# ─────────────────────────────────────────────────────────── wildfly
if [[ " $* " == *" --todo "* ]]; then
  titulo "3 · WAR y WildFly"

  if [ ! -d "$WILDFLY" ]; then
    rojo "  no encontré WildFly en $WILDFLY"
  else
    # El JDK 17 es el que está probado con Jakarta EE 10 en este proyecto.
    for jdk in /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/java-1.17.0-openjdk-amd64; do
      [ -d "$jdk" ] && export JAVA_HOME="$jdk" && break
    done
    gris "  JAVA_HOME=${JAVA_HOME:-<sin definir>}"

    gris "  compilando el WAR (tarda ~1 min la primera vez)…"
    if ( cd "$RAIZ" && ./mvnw -q clean package ); then
      cp "$RAIZ/target/ecommerce.war" "$WILDFLY/standalone/deployments/"
      verde "  OK  WAR desplegado — el front del WAR queda igual al de la demo"
    else
      rojo  "  el build falló. Usá el WildFly sólo para el log (no abras /ecommerce/)"
    fi

    titulo "   Arrancando WildFly — dejá ESTA terminal abierta"
    gris "   Esperá la línea 'WFLYSRV0025: WildFly ... started'"
    gris "   La línea que tenés que mostrar es:"
    gris "     Catálogo cargado: 16 productos, 5 categorías"
    echo
    exec "$WILDFLY/bin/standalone.sh"
  fi
fi

# ─────────────────────────────────────────────────────────── navegador
titulo "4 · Navegador"
if command -v xdg-open >/dev/null; then
  xdg-open "http://localhost:$PUERTO/" >/dev/null 2>&1 &
  verde "  abierto"
else
  gris "  abrilo a mano: http://localhost:$PUERTO/"
fi

# ─────────────────────────────────────────────────────────── a mano
cat <<'FIN'

╭──────────────────────────────────────────────────────────────╮
│  ESTO VA A MANO — es lo que siempre se olvida               │
╰──────────────────────────────────────────────────────────────╯

  1. En la consola del navegador (F12):

        localStorage.clear(); location.reload();

     Sin esto arrancás con sesión y el 401 de Tomás no se ve.

  2. Agrandá las fuentes, que se proyecta:
       · editor   18-20 pt
       · terminal 16-18 pt
       · consola de DevTools  ← tiene su tamaño APARTE, no te olvides

  3. Cerrá notificaciones, WhatsApp y pestañas de más.

  4. Cada uno maneja el teclado en SU bloque.

  Para bajar el front cuando termines:
FIN
printf '        kill %s\n\n' "$FRONT_PID"

verde "Listo. El front sigue corriendo en segundo plano."

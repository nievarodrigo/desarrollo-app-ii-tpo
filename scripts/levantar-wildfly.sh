#!/usr/bin/env bash
#
# Compila el WAR y levanta WildFly (con JMS) sin sudo.
#
#   JAVA_HOME=~/.local/jdk WILDFLY_HOME=~/.local/wildfly/wildfly-41.0.1.Final ./scripts/levantar-wildfly.sh
#
# Variables:
#   JAVA_HOME             JDK 17+ (por defecto ~/.local/jdk si existe)
#   WILDFLY_HOME          carpeta de WildFly 41 (por defecto ~/.local/wildfly/wildfly-41.0.1.Final)
#   GOMARKET_JWT_SECRET   clave para firmar los JWT (32+ caracteres). Si falta, se genera una al azar en .secretos/jwt.
#   GOMARKET_IA_PROVEEDOR proveedor del gateway de IA: "anthropic" (por defecto, Claude Haiku; necesita
#                         ANTHROPIC_API_KEY) o "local" (Ollama en 127.0.0.1:11434 con qwen2.5:3b, costo cero).
#                         Sin proveedor disponible, el gateway responde con el fallback de reglas.
#
#   Demo con el modelo local:
#     ~/.local/ollama/bin/ollama serve &                       # en otra terminal
#     GOMARKET_IA_PROVEEDOR=local ./scripts/levantar-wildfly.sh
#
# Se baja con Ctrl+C. Si WildFly no está instalado:
#   curl -L -o wf.tar.gz https://github.com/wildfly/wildfly/releases/download/41.0.1.Final/wildfly-41.0.1.Final.tar.gz
#   mkdir -p ~/.local/wildfly && tar xzf wf.tar.gz -C ~/.local/wildfly

set -euo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/.local/jdk}"
export PATH="$JAVA_HOME/bin:$PATH"
WF="${WILDFLY_HOME:-$HOME/.local/wildfly/wildfly-41.0.1.Final}"
[ -x "$WF/bin/standalone.sh" ] || { echo "No encuentro WildFly en $WF (definí WILDFLY_HOME)"; exit 1; }

# Sin secretos en el repo: si GOMARKET_JWT_SECRET no viene del entorno, se genera una clave
# aleatoria UNA vez y se guarda en .secretos/ (ignorado por git), así sobrevive a reinicios.
SECRETOS="$RAIZ/.secretos"; mkdir -p "$SECRETOS"; chmod 700 "$SECRETOS"
if [ -z "${GOMARKET_JWT_SECRET:-}" ]; then
  [ -s "$SECRETOS/jwt" ] || { head -c 48 /dev/urandom | base64 | tr -d '\n' > "$SECRETOS/jwt"; chmod 600 "$SECRETOS/jwt"; }
  GOMARKET_JWT_SECRET="$(cat "$SECRETOS/jwt")"
fi
export GOMARKET_JWT_SECRET

if [ "${GOMARKET_IA_PROVEEDOR:-}" = "local" ]; then
  if curl -s -m 2 "${GOMARKET_IA_LOCAL_URL:-http://127.0.0.1:11434}/api/tags" >/dev/null; then
    echo "IA: modelo local (Ollama) · ${GOMARKET_IA_LOCAL_MODELO:-qwen2.5:3b}"
  else
    echo "AVISO: GOMARKET_IA_PROVEEDOR=local pero Ollama no responde; la búsqueda usará el fallback de reglas."
  fi
fi

( cd "$RAIZ" && ./mvnw -o -q -B -DskipTests package )
# Reintentos + DLQ + persistencia de la cola de pedidos (idempotente, sin servidor corriendo)
"$WF/bin/jboss-cli.sh" --file="$RAIZ/scripts/datasource.cli" >/dev/null
"$WF/bin/jboss-cli.sh" --file="$RAIZ/scripts/mensajeria.cli" >/dev/null
rm -f "$WF"/standalone/deployments/ecommerce.war*
cp "$RAIZ/target/ecommerce.war" "$WF/standalone/deployments/"
exec "$WF/bin/standalone.sh" -c standalone-full.xml   # standalone-full habilita JMS/Artemis

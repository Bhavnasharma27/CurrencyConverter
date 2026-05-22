#!/usr/bin/env bash
# ─── compile.sh ─────────────────────────────────────────────────────────────
# Compiles all Java sources with the MySQL JDBC driver on the classpath.
# Run once before launching the app.
# ────────────────────────────────────────────────────────────────────────────

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
OUT_DIR="$SCRIPT_DIR/out"
LIB_DIR="$SCRIPT_DIR/lib"

JAR=$(ls "$LIB_DIR"/mysql-connector-j-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
    echo "[ERROR] MySQL JDBC driver not found in lib/."
    echo "        Run: curl -L 'https://search.maven.org/remotecontent?filepath=com/mysql/mysql-connector-j/9.3.0/mysql-connector-j-9.3.0.jar' -o lib/mysql-connector-j-9.3.0.jar"
    exit 1
fi

mkdir -p "$OUT_DIR"

echo "[INFO] Compiling sources..."
find "$SRC_DIR" -name "*.java" | xargs javac -cp "$JAR" -d "$OUT_DIR"
echo "[OK]   Compiled to $OUT_DIR"

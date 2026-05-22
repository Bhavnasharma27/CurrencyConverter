#!/usr/bin/env bash
# ─── run.sh ─────────────────────────────────────────────────────────────────
# Launches the Currency Converter Swing UI.
# Make sure you ran compile.sh first.
# ────────────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/out"
LIB_DIR="$SCRIPT_DIR/lib"

JAR=$(ls "$LIB_DIR"/mysql-connector-j-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
    echo "[ERROR] MySQL JDBC driver not found in lib/. Run compile.sh first."
    exit 1
fi

if [ ! -d "$OUT_DIR/com" ]; then
    echo "[ERROR] Compiled classes not found. Run compile.sh first."
    exit 1
fi

echo "[INFO] Starting Currency Converter UI..."
java -cp "$OUT_DIR:$JAR" com.currency.ui.CurrencyConverterUI

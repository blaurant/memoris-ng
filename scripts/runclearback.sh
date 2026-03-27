#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
XTDB_DIR="$BACKEND_DIR/data/xtdb"
PORT=3000

echo "=== Backend with DB clear (port $PORT) ==="

# Stop existing process
PID=$(lsof -ti:$PORT 2>/dev/null || true)
if [ -n "$PID" ]; then
  echo "Stopping existing process (PID $PID)..."
  kill -9 $PID 2>/dev/null || true
  sleep 1
fi

# Clear XTDB database
if [ -d "$XTDB_DIR" ]; then
  echo "Clearing XTDB database ($XTDB_DIR)..."
  rm -rf "$XTDB_DIR"
else
  echo "No XTDB data directory to clear."
fi

# Start backend with .env
echo "Starting backend..."
cd "$BACKEND_DIR"
export $(cat .env | grep -v '^#' | xargs)
clj -M -m main &
BACK_PID=$!
echo "Backend started (PID $BACK_PID)"

# Wait for startup
for i in $(seq 1 30); do
  if curl -s http://localhost:$PORT/api/v1/hello >/dev/null 2>&1; then
    echo "Backend ready on http://localhost:$PORT (fresh DB)"
    exit 0
  fi
  sleep 1
done

echo "Warning: backend did not respond after 30s (PID $BACK_PID still running)"

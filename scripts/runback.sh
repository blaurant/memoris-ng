#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
PORT=3000

echo "=== Backend (port $PORT) ==="

# Stop existing process
PID=$(lsof -ti:$PORT 2>/dev/null || true)
if [ -n "$PID" ]; then
  echo "Stopping existing process (PID $PID)..."
  kill -9 $PID 2>/dev/null || true
  sleep 1
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
    echo "Backend ready on http://localhost:$PORT"
    exit 0
  fi
  sleep 1
done

echo "Warning: backend did not respond after 30s (PID $BACK_PID still running)"

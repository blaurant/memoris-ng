#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
PORT=3449
SHADOW_PORT=9630

echo "=== Frontend (port $PORT) ==="

# Stop existing processes
PIDS=$(lsof -ti:$PORT -ti:$SHADOW_PORT 2>/dev/null || true)
if [ -n "$PIDS" ]; then
  echo "Stopping existing processes ($PIDS)..."
  echo "$PIDS" | xargs kill -9 2>/dev/null || true
  sleep 1
fi

# Start frontend with .env
echo "Starting frontend..."
cd "$FRONTEND_DIR"
export $(cat .env | grep -v '^#' | xargs)
npm run dev &
FRONT_PID=$!
echo "Frontend started (PID $FRONT_PID)"

# Wait for startup
for i in $(seq 1 40); do
  if curl -s http://localhost:$PORT >/dev/null 2>&1; then
    echo "Frontend ready on http://localhost:$PORT"
    exit 0
  fi
  sleep 1
done

echo "Warning: frontend did not respond after 40s (PID $FRONT_PID still running)"

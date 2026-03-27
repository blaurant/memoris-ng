#!/usr/bin/env bash

BACK_PORT=3000
FRONT_PORT=3449
SHADOW_PORT=9630

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "=== Status ==="
echo ""

# Backend
BACK_PID=$(lsof -ti:$BACK_PORT 2>/dev/null || true)
if [ -n "$BACK_PID" ]; then
  BACK_RESP=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$BACK_PORT/api/v1/hello 2>/dev/null || echo "000")
  echo -e "Backend:  ${GREEN}UP${NC}  (PID $BACK_PID, port $BACK_PORT, HTTP $BACK_RESP)"
else
  echo -e "Backend:  ${RED}DOWN${NC}  (port $BACK_PORT libre)"
fi

# Frontend
FRONT_PID=$(lsof -ti:$FRONT_PORT 2>/dev/null || true)
if [ -n "$FRONT_PID" ]; then
  FRONT_RESP=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$FRONT_PORT 2>/dev/null || echo "000")
  echo -e "Frontend: ${GREEN}UP${NC}  (PID $FRONT_PID, port $FRONT_PORT, HTTP $FRONT_RESP)"
else
  echo -e "Frontend: ${RED}DOWN${NC}  (port $FRONT_PORT libre)"
fi

# Shadow-cljs UI
SHADOW_PID=$(lsof -ti:$SHADOW_PORT 2>/dev/null || true)
if [ -n "$SHADOW_PID" ]; then
  echo -e "Shadow:   ${GREEN}UP${NC}  (PID $SHADOW_PID, port $SHADOW_PORT)"
else
  echo -e "Shadow:   ${RED}DOWN${NC}  (port $SHADOW_PORT libre)"
fi

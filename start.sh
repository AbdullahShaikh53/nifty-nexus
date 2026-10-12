#!/usr/bin/env bash
# One-command start: backend (embedded H2, no MySQL needed) + frontend.
set -e
cd "$(dirname "$0")"
PROFILE=${1:-h2}   # ./start.sh mysql  -> uses docker-compose MySQL
if [ "$PROFILE" = "mysql" ]; then docker compose up -d mysql; PROFILE=default; fi
(cd backend && ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=$PROFILE) &
(cd frontend && npm install && npm run dev -- --open) &
wait

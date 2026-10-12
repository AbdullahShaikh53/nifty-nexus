@echo off
title Nifty Nexus
cd /d "%~dp0"
echo Starting Nifty Nexus: backend on http://localhost:8080 (embedded H2) and frontend on http://localhost:5173
start "Nifty Nexus Backend" cmd /k "cd /d "%~dp0backend" && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2"
start "Nifty Nexus Frontend" cmd /k "cd /d "%~dp0frontend" && npm install && npm run dev -- --open"

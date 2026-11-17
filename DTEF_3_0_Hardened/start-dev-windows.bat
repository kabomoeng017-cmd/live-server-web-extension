@echo off
setlocal
if not exist .env copy .env.example .env
start "DTEF Infrastructure" cmd /k "docker compose up -d postgres redis rabbitmq"
start "DTEF Backend" cmd /k "cd backend && set SPRING_PROFILES_ACTIVE=demo && mvn spring-boot:run"
start "DTEF Frontend" cmd /k "cd frontend && npm ci && npm run dev"
echo DTEF local development startup commands opened in separate terminals.
echo Frontend: http://localhost:3000
endlocal

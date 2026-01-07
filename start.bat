@echo off
echo ========================================
echo Iniciando Fin System
echo ========================================
echo.

echo Parando containers existentes...
docker-compose down

echo.
echo Construindo e iniciando containers...
docker-compose up -d --build

echo.
echo Aguardando servicos iniciarem...
timeout /t 10 /nobreak >nul

echo.
echo Verificando status dos containers...
docker-compose ps

echo.
echo ========================================
echo Sistema iniciado!
echo ========================================
echo.
echo Acesse:
echo - Frontend: http://localhost:3000
echo - Backend API: http://localhost:8080/api
echo.
echo Para ver os logs:
echo   docker-compose logs -f
echo.
echo Para parar:
echo   docker-compose down
echo.
pause











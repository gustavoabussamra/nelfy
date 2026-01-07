Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Iniciando Fin System" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Parando containers existentes..." -ForegroundColor Yellow
docker-compose down

Write-Host ""
Write-Host "Construindo e iniciando containers..." -ForegroundColor Yellow
docker-compose up -d --build

Write-Host ""
Write-Host "Aguardando serviços iniciarem..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "Verificando status dos containers..." -ForegroundColor Yellow
docker-compose ps

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Sistema iniciado!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Acesse:" -ForegroundColor White
Write-Host "- Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "- Backend API: http://localhost:8080/api" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para ver os logs:" -ForegroundColor White
Write-Host "  docker-compose logs -f" -ForegroundColor Gray
Write-Host ""
Write-Host "Para parar:" -ForegroundColor White
Write-Host "  docker-compose down" -ForegroundColor Gray
Write-Host ""











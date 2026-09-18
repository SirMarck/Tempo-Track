@echo off
title Publicando Atualizacao TempoTrack v2.3.0
echo ======================================================
echo    PUBLICANDO TEMPOTRACK v2.3.0 PARA O GITHUB
echo ======================================================
echo.
cd /d "%~dp0"
echo Enviando commits e tags para o GitHub...
git push origin main --tags
echo.
echo ======================================================
if %ERRORLEVEL% EQU 0 (
    echo [SUCESSO] Atualizacao enviada com sucesso!
    echo A release v2.3.0 esta sendo gerada no GitHub Actions.
) else (
    echo [ERRO] Ocorreu um erro no envio.
)
echo ======================================================
pause

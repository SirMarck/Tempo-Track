@echo off
title Publicando Atualizacao TempoTrack v2.4.2
echo ======================================================
echo    PUBLICANDO ATUALIZACAO TEMPOTRACK v2.4.2
echo ======================================================
echo.
cd /d "%~dp0"
echo Enviando commits e atualizando tag no GitHub...
git push origin main --tags -f
echo.
echo ======================================================
if %ERRORLEVEL% EQU 0 (
    echo [SUCESSO] Atualizacao enviada com sucesso!
    echo A nova release v2.4.2 esta sendo compilada no GitHub Actions.
) else (
    echo [ERRO] Ocorreu um erro no envio.
)
echo ======================================================
pause

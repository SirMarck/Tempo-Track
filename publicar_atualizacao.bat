@echo off
title Publicando Correcao TempoTrack v2.3.0
echo ======================================================
echo    PUBLICANDO CORRECAO TEMPOTRACK v2.3.0
echo ======================================================
echo.
cd /d "%~dp0"
echo Enviando commits e atualizando tag no GitHub...
git push origin main --tags -f
echo.
echo ======================================================
if %ERRORLEVEL% EQU 0 (
    echo [SUCESSO] Correcao enviada com sucesso!
    echo A nova release v2.3.0 esta sendo compilada no GitHub Actions.
) else (
    echo [ERRO] Ocorreu um erro no envio.
)
echo ======================================================
pause

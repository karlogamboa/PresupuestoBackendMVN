@echo off
REM Script para compilar y desplegar la aplicación Spring Boot con HTTPS en Ubuntu desde Windows
REM Uso: deploy-ubuntu-https.bat <JAR_FILE> <KEYSTORE_FILE> <KEYSTORE_PASSWORD>

REM Variables de entrada
set JAR_FILE=%1
if "%JAR_FILE%"=="" set JAR_FILE=target\presupuesto-backend-1.0.0-SNAPSHOT.jar
set KEYSTORE_FILE=%2
if "%KEYSTORE_FILE%"=="" set KEYSTORE_FILE=presupuesto-https.p12
set KEYSTORE_PASSWORD=%3
if "%KEYSTORE_PASSWORD%"=="" set KEYSTORE_PASSWORD=password

REM Configuración de usuario y host
set SERVER_USER=ubuntu
set SERVER_IP=ec2-3-16-137-159.us-east-2.compute.amazonaws.com
set SSH_KEY=%USERPROFILE%\.ssh\presupuesto-key-cdc.pem
set DEST_DIR=presupuesto-app

REM 1. Compilar el JAR
call mvn clean package -DskipTests
if not exist %JAR_FILE% (
    echo ERROR: No se generó el archivo JAR: %JAR_FILE%
    pause
    exit /b 1
)


REM 2. Crear directorio remoto si no existe y borrar el JAR y application.properties anteriores si existen
ssh -i "%SSH_KEY%" %SERVER_USER%@%SERVER_IP% "if [ ! -d ~/%DEST_DIR% ]; then mkdir -p ~/%DEST_DIR%; fi; if [ -f ~/%DEST_DIR%/presupuesto-backend-1.0.0-SNAPSHOT.jar ]; then rm ~/%DEST_DIR%/presupuesto-backend-1.0.0-SNAPSHOT.jar; fi; if [ -f ~/%DEST_DIR%/application.properties ]; then rm ~/%DEST_DIR%/application.properties; fi"

REM 2b. Matar cualquier proceso que esté usando el puerto 8443 en el servidor remoto
ssh -i "%SSH_KEY%" %SERVER_USER%@%SERVER_IP% "sudo fuser -k 8443/tcp || true"

REM 3. Copiar JAR al servidor
scp -i "%SSH_KEY%" %JAR_FILE% %SERVER_USER%@%SERVER_IP%:~/%DEST_DIR%/

REM Copiar el keystore solo si no existe
ssh -i "%SSH_KEY%" %SERVER_USER%@%SERVER_IP% "if [ ! -f ~/%DEST_DIR%/%KEYSTORE_FILE% ]; then exit 99; fi"
if %ERRORLEVEL%==99 (
    scp -i "%SSH_KEY%" %KEYSTORE_FILE% %SERVER_USER%@%SERVER_IP%:~/%DEST_DIR%/
)


REM 4. Copiar siempre el archivo application.properties local al servidor
scp -i "%SSH_KEY%" src/main/resources/application.properties %SERVER_USER%@%SERVER_IP%:~/%DEST_DIR%/

REM 5. Ejecutar el script de actualización en el servidor
ssh -i "%SSH_KEY%" %SERVER_USER%@%SERVER_IP% "cd ~/%DEST_DIR% && bash ~/presupuesto-scripts/update-app.sh"

echo Despliegue iniciado en Ubuntu con HTTPS.
pause

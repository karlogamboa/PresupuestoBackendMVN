@echo off
REM Setup Ubuntu Management Scripts
REM This script uploads and executes the Ubuntu scripts creation on the server

echo =================================
echo   SETUP UBUNTU SCRIPTS
echo =================================
echo.

REM Configuration
set SERVER_USER=ubuntu
set SERVER_IP=ec2-3-16-137-159.us-east-2.compute.amazonaws.com
set SSH_KEY=%USERPROFILE%\.ssh\presupuesto-key-cdc.pem
set REMOTE_DIR=/home/ubuntu/

REM Check if SSH key exists
if not exist "%SSH_KEY%" (
    echo ERROR: SSH key not found at %SSH_KEY%
    echo Please ensure your SSH key is placed in the correct location.
    pause
    exit /b 1
)

REM Check if create-ubuntu-scripts.sh exists
if not exist "create-ubuntu-scripts.sh" (
    echo ERROR: create-ubuntu-scripts.sh not found in current directory
    echo Please ensure the script file is in the project root.
    pause
    exit /b 1
)

echo Setting up Ubuntu management scripts on %SERVER_USER%@%SERVER_IP%...
echo.

REM Create remote directory
echo Creating remote directory...
ssh -i "%SSH_KEY%" %SERVER_USER%@%SERVER_IP% "mkdir -p %REMOTE_DIR%"

REM Upload the Ubuntu scripts creator
echo Uploading Ubuntu scripts creator...
scp -i "%SSH_KEY%" create-ubuntu-scripts.sh %SERVER_USER%@%SERVER_IP%:%REMOTE_DIR%/

REM Execute the script on Ubuntu
echo Executing Ubuntu scripts setup...
ssh -i "%SSH_KEY%" %SERVER_USER%@%SERVER_IP% "cd %REMOTE_DIR% && chmod +x create-ubuntu-scripts.sh && ./create-ubuntu-scripts.sh"

REM Verify scripts were created
echo Verifying scripts were created...
ssh -i "%SSH_KEY%" %SERVER_USER%@%SERVER_IP% "ls -la ~/presupuesto-scripts/"

echo.
echo =================================
echo Ubuntu scripts setup completed!
echo =================================
echo.
echo Available Ubuntu scripts:
echo - ~/presupuesto-scripts/deploy-presupuesto.sh
echo - ~/presupuesto-scripts/status-presupuesto.sh
echo - ~/presupuesto-scripts/logs-presupuesto.sh
echo - ~/presupuesto-scripts/restart-presupuesto.sh
echo - ~/presupuesto-scripts/update-presupuesto.sh
echo - ~/presupuesto-scripts/setup-nginx.sh
echo - ~/presupuesto-scripts/system-info.sh
echo - ~/presupuesto-scripts/check-install-java.sh
echo.
echo Next steps:
echo ssh -i "%SSH_KEY%" %SERVER_USER%@%SERVER_IP%
echo  ~/presupuesto-scripts/check-install-java.sh
echo  ~/presupuesto-scripts/deploy-presupuesto.sh
echo  ~/presupuesto-scripts/check-status.sh
echo  ~/presupuesto-scripts/logs-presupuesto.sh
echo  ~/presupuesto-scripts/restart-presupuesto.sh
echo  ~/presupuesto-scripts/update-presupuesto.sh
echo  ~/presupuesto-scripts/setup-nginx.sh
echo  ~/presupuesto-scripts/system-info.sh
echo  sudo systemctl stop presupuesto 
echo.

pause
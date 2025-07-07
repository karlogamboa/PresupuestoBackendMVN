@echo off
REM Configure Function URL for Lambda to enable direct HTTP access
set FUNCTION_NAME=fin-lamba-presupuesto
set REGION=us-east-2

echo Configuring Lambda Function URL...
echo.

REM Create function URL configuration
aws lambda create-function-url-config ^
    --function-name %FUNCTION_NAME% ^
    --region %REGION% ^
    --cors "AllowCredentials=true,AllowMethods=GET,POST,PUT,DELETE,OPTIONS,AllowOrigins=*,AllowHeaders=*,MaxAge=3600" ^
    --auth-type NONE

if %errorlevel% equ 0 (
    echo [✓] Function URL created successfully
    echo.
    echo Getting function URL...
    aws lambda get-function-url-config --function-name %FUNCTION_NAME% --region %REGION%
    echo.
    echo Your Lambda function is now accessible via HTTP!
    echo.
    echo To access Swagger UI, use:
    echo https://YOUR_FUNCTION_URL/swagger-ui.html
    echo.
    echo To access OpenAPI docs, use:
    echo https://YOUR_FUNCTION_URL/v3/api-docs
) else (
    echo Function URL might already exist. Getting existing URL...
    aws lambda get-function-url-config --function-name %FUNCTION_NAME% --region %REGION%
)

echo.
echo Additional endpoints to test:
echo - Health: https://YOUR_FUNCTION_URL/health
echo - User Info: https://YOUR_FUNCTION_URL/api/userInfo
echo - Solicitudes: https://YOUR_FUNCTION_URL/api/solicitudes-presupuesto
echo - Areas: https://YOUR_FUNCTION_URL/api/areas
echo - Swagger UI: https://YOUR_FUNCTION_URL/swagger-ui.html

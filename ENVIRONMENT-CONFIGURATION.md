# Configuración de Ambientes - Sistema de Presupuestos

Este documento explica cómo configurar y usar los diferentes ambientes del sistema de presupuestos.

## Ambientes Disponibles


### 1. **Desarrollo (dev)**
- **Perfil Spring**: `dev`
- **Parameter Store**: `/fin/dev/presupuesto/*`
- **Tablas DynamoDB**: `fin-dynamodb-dev-presupuesto-*`
- **Características**:
  - Autenticación deshabilitada por defecto
  - CORS permisivo (localhost)
  - Logging nivel DEBUG

### 2. **QA/Testing (qa)**
- **Perfil Spring**: `qa`
- **Parameter Store**: `/fin/qa/presupuesto/*`
- **Tablas DynamoDB**: `fin-dynamodb-qa-presupuesto-*`
- **Características**:
  - Autenticación habilitada
  - CORS configurado para QA frontend
  - Logging nivel INFO

### 3. **Producción (prod)**
- **Perfil Spring**: `prod`
- **Parameter Store**: `/fin/prod/presupuesto/*`
- **Tablas DynamoDB**: `fin-dynamodb-prod-presupuesto-*`
- **Características**:
  - Autenticación habilitada
  - CORS estricto
  - Logging nivel WARN

## Configuración por Ambiente

### Ejecución Local (Solo para Desarrollo)

> **Nota**: La ejecución local solo se recomienda para desarrollo. El sistema está diseñado para ejecutarse en AWS Lambda.

#### Desarrollo Local:
```bash
java -jar target/presupuesto-backend.jar --spring.profiles.active=dev
# Se ejecuta en http://localhost:8080 sin SSL
```

#### Testing Local (simulando QA):
```bash
java -jar target/presupuesto-backend.jar --spring.profiles.active=qa
# Se ejecuta en http://localhost:8080 con configuración de QA
```

### AWS Lambda (Ambiente Principal)

El sistema está diseñado para ejecutarse principalmente en AWS Lambda. Para Lambda, se usa la variable de ambiente `ENVIRONMENT`:

#### QA Lambda:
```bash
export ENVIRONMENT=qa
export SPRING_PROFILES_ACTIVE=lambda,qa
```

#### Producción Lambda:
```bash
export ENVIRONMENT=prod
export SPRING_PROFILES_ACTIVE=lambda,prod
```

#### Desarrollo en Lambda (para pruebas):
```bash
export ENVIRONMENT=dev
export SPRING_PROFILES_ACTIVE=lambda,dev
```

## Estructura de Parameter Store

```
/fin/
├── dev/
│   └── presupuesto/
│       ├── aws/
│       ├── security/
│       ├── api-gateway/
│       └── cors/
├── qa/
│   └── presupuesto/
│       ├── aws/
│       ├── security/
│       ├── api-gateway/
│       └── cors/
└── prod/
    └── presupuesto/
        ├── aws/
        ├── security/
        ├── api-gateway/
        └── cors/
```

## Configuración de DynamoDB por Ambiente

### Desarrollo
- Prefijo: `fin-dynamodb-dev-presupuesto-`
- Tablas: `fin-dynamodb-dev-presupuesto-solicitudes`, etc.

### QA
- Prefijo: `fin-dynamodb-qa-presupuesto-`
- Tablas: `fin-dynamodb-qa-presupuesto-solicitudes`, etc.

### Producción
- Prefijo: `fin-dynamodb-prod-presupuesto-`
- Tablas: `fin-dynamodb-prod-presupuesto-solicitudes`, etc.

## Variables de Entorno para Lambda

### Variables Requeridas:
- `ENVIRONMENT`: `dev`, `qa`, o `prod`
- `SPRING_PROFILES_ACTIVE`: `lambda,{environment}`
- `AWS_REGION`: Región de AWS (ej: `us-east-2`)

### Variables Opcionales:
- `TABLE_PREFIX`: Sobrescribe el prefijo de tablas DynamoDB
- `CORS_ALLOWED_ORIGINS`: Sobrescribe los orígenes CORS permitidos

## Ejemplo de Configuración en AWS Lambda

```json
{
  "Environment": {
    "Variables": {
      "ENVIRONMENT": "qa",
      "SPRING_PROFILES_ACTIVE": "lambda,qa",
      "AWS_REGION": "us-east-2"
    }
  }
}
```

## Verificación de Configuración

### Ver parámetros actuales:
```bash
aws ssm get-parameters-by-path --path "/fin/qa/presupuesto" --recursive
```

### Ver tablas DynamoDB:
```bash
aws dynamodb list-tables --query "TableNames[?starts_with(@, 'fin-dynamodb-qa-presupuesto')]"
```

## Arquitectura Lambda-First

Este sistema está diseñado específicamente para AWS Lambda con las siguientes características:



## Endpoints de Autenticación y Sesión

El sistema soporta los siguientes endpoints relacionados con autenticación y sesión:

- `POST /api/userInfo` - Obtiene la información del usuario actualmente autenticado.
- `POST /api/logout` - Invalida la sesión del usuario.

> **Nota:**
> - En la arquitectura actual, `/api/userInfo` y `/api/logout` están implementados y gestionan la sesión del usuario autenticado a través del API Gateway Authorizer.
> - Los endpoints `/auth/login` y `/auth/callback` ya no existen en el backend (el login es responsabilidad del API Gateway Authorizer).


---

### Seguridad del Flujo OAuth2

- El backend implementa el flujo **OAuth2 Authorization Code Flow**. Esto garantiza que los tokens de Okta y el `client_secret` nunca se expongan al navegador.
- Después de una autenticación exitosa, el backend genera su propio **JWT de sesión**. Este token se utiliza para proteger los endpoints de la API, en lugar de usar los tokens de Okta directamente en el cliente.

### Flujo típico:

1. El usuario accede a `/auth/login` y es redirigido a Okta para autenticación.
2. Okta redirige a `/auth/callback` con un código de autorización.
3. El backend intercambia el código por tokens y establece la sesión.
4. El frontend puede consultar `/api/userInfo` para obtener los datos del usuario autenticado.
5. El usuario puede cerrar sesión con `/api/logout`.

### ✅ Optimizaciones para Lambda
- **Inicialización Lazy**: `spring.main.lazy-initialization=true`
- **Sin configuración de servidor**: Puerto y SSL manejados por API Gateway
- **Gestión de estado**: Diseño stateless completo
- **Cold start optimizado**: Dependencias mínimas y configuración eficiente
- **Parameter Store**: Configuración dinámica sin redeploy

### 🔄 API Gateway Integration
- **Routing**: Manejo de todas las rutas HTTP
- **CORS**: Configurado a nivel de API Gateway
- **SSL/TLS**: Terminación SSL en API Gateway
- **Authentication**: API Gateway Authorizer (no JWT local)
- **Rate Limiting**: Controlado por API Gateway

### 📦 Despliegue
- **JAR optimizado**: Maven Shade Plugin para Lambda
- **Layers**: Dependencias separadas para deployments rápidos
- **Environment Variables**: Configuración por ambiente
- **Parameter Store**: Configuración sensible centralizada

# Configuración de Ambientes - Sistema de Presupuestos

Este documento explica cómo configurar y usar los diferentes ambientes del sistema de presupuestos.

## Ambientes Disponibles

### 1. **Desarrollo (dev)**
- **Perfil Spring**: `dev`
- **Parameter Store**: `/fin/dev/presupuesto/*`
- **Tablas DynamoDB**: `fin-dynamodb-dev-presupuesto-*`
- **Características**:
  - Autenticación deshabilitada por defecto
  - Swagger UI habilitado
  - CORS permisivo (localhost)
  - Logging nivel DEBUG

### 2. **QA/Testing (qa)**
- **Perfil Spring**: `qa`
- **Parameter Store**: `/fin/qa/presupuesto/*`
- **Tablas DynamoDB**: `fin-dynamodb-qa-presupuesto-*`
- **Características**:
  - Autenticación habilitada
  - Swagger UI habilitado para pruebas
  - CORS configurado para QA frontend
  - Logging nivel INFO

### 3. **Producción (prod)**
- **Perfil Spring**: `prod`
- **Parameter Store**: `/fin/prod/presupuesto/*`
- **Tablas DynamoDB**: `fin-dynamodb-prod-presupuesto-*`
- **Características**:
  - Autenticación habilitada
  - Swagger UI deshabilitado
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

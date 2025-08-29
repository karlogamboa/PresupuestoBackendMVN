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

### AWS App Runner (Ambiente Principal)

El sistema está diseñado para ejecutarse principalmente en AWS App Runner. Para App Runner, las variables de entorno se configuran en el servicio:

#### QA App Runner:
```bash
export ENVIRONMENT=qa
export SPRING_PROFILES_ACTIVE=apprunner,qa
export AWS_REGION=us-east-2
export SERVER_PORT=8080
```

#### Producción App Runner:
```bash
export ENVIRONMENT=prod
export SPRING_PROFILES_ACTIVE=apprunner,prod
export AWS_REGION=us-east-2
export SERVER_PORT=8080
```

#### Desarrollo App Runner (para pruebas):
```bash
export ENVIRONMENT=dev
export SPRING_PROFILES_ACTIVE=apprunner,dev
export AWS_REGION=us-east-2
export SERVER_PORT=8080
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
- Tablas: `fin-dynamodb-dev-presupuesto-solicitudes`, `fin-dynamodb-dev-presupuesto-solicitantes`, `fin-dynamodb-dev-presupuesto-proveedores`, `fin-dynamodb-dev-presupuesto-departamentos`, `fin-dynamodb-dev-presupuesto-categorias-gasto`, `fin-dynamodb-dev-presupuesto-scim-users`, `fin-dynamodb-dev-presupuesto-scim-groups`, etc.

### QA
- Prefijo: `fin-dynamodb-qa-presupuesto-`
- Tablas: `fin-dynamodb-qa-presupuesto-solicitudes`, `fin-dynamodb-qa-presupuesto-solicitantes`, `fin-dynamodb-qa-presupuesto-proveedores`, `fin-dynamodb-qa-presupuesto-departamentos`, `fin-dynamodb-qa-presupuesto-categorias-gasto`, `fin-dynamodb-qa-presupuesto-scim-users`, `fin-dynamodb-qa-presupuesto-scim-groups`, etc.

### Producción
- Prefijo: `fin-dynamodb-prod-presupuesto-`
- Tablas: `fin-dynamodb-prod-presupuesto-solicitudes`, `fin-dynamodb-prod-presupuesto-solicitantes`, `fin-dynamodb-prod-presupuesto-proveedores`, `fin-dynamodb-prod-presupuesto-departamentos`, `fin-dynamodb-prod-presupuesto-categorias-gasto`, `fin-dynamodb-prod-presupuesto-scim-users`, `fin-dynamodb-prod-presupuesto-scim-groups`, etc.

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
- `POST /login` - Invalida la sesión del usuario.



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
- **Authentication**: SAML2 (no API Gateway Authorizer, no JWT local)
- **Rate Limiting**: Controlado por API Gateway

### 📦 Despliegue
- **JAR optimizado**: Maven Shade Plugin para Lambda
- **Layers**: Dependencias separadas para deployments rápidos
- **Environment Variables**: Configuración por ambiente
- **Parameter Store**: Configuración sensible centralizada

## Endpoints SCIM para Aprovisionamiento (Okta)

El backend expone endpoints compatibles con SCIM 2.0 para integración con Okta u otros IdPs:

- `GET /scim/v2/Users` - Lista usuarios
- `POST /scim/v2/Users` - Crea usuario
- `GET /scim/v2/Users/{id}` - Obtiene usuario por ID
- `PUT /scim/v2/Users/{id}` - Reemplaza usuario
- `PATCH /scim/v2/Users/{id}` - Modifica usuario parcialmente
- `DELETE /scim/v2/Users/{id}` - Elimina usuario

- `GET /scim/v2/Groups` - Lista grupos
- `POST /scim/v2/Groups` - Crea grupo
- `GET /scim/v2/Groups/{id}` - Obtiene grupo por ID
- `PUT /scim/v2/Groups/{id}` - Reemplaza grupo
- `PATCH /scim/v2/Groups/{id}` - Modifica grupo parcialmente
- `DELETE /scim/v2/Groups/{id}` - Elimina grupo

> **Nota:** Los endpoints SCIM usan DynamoDB (`scim-users`, `scim-groups`) y están protegidos por autenticación (por ejemplo, Basic Auth para Okta provisioning).
> **Importante:** El atributo `"id"` debe estar presente en todos los objetos usuario SCIM para que Okta pueda aprovisionar correctamente.

- **Stage:** La propiedad `stage` se define en cada archivo de configuración por ambiente (`application.properties`, `application-qa.properties`, `application-prod.properties`) y se utiliza en el backend para rutas y redirecciones dinámicas.

> **Nota:** Para SAML2, asegúrate que la configuración de CORS permita credenciales (`allowCredentials=true`) y que el navegador envíe las cookies de sesión en las solicitudes a endpoints protegidos. Spring Security procesa automáticamente las cookies de sesión si la política de sesión lo permite.

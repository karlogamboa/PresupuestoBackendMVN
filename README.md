# Sistema de Gestión de Presupuestos - Backend

Este es el repositorio del backend para el sistema de gestión de presupuestos. Provee las APIs necesarias para la creación, consulta y administración de solicitudes de presupuesto, así como la gestión de proveedores, departamentos y categorías de gasto.

## Descripción General

El sistema está diseñado para integrarse con AWS, utilizando servicios como DynamoDB, S3 y SES. También se integra con Okta para la autenticación y autorización de usuarios.

- **DynamoDB**: Base de datos NoSQL, con prefijos de tabla por ambiente (`dev`, `qa`, `prod`, `lambda`).  
  **Tablas principales:**  
  - `solicitudes`  
  - `proveedores`  
  - `departamentos`  
  - `categorias-gasto`  
  - `scim-users`  
  - `scim-groups`

- **S3**: Almacenamiento de archivos adjuntos a las solicitudes de presupuesto.

- **SES (Simple Email Service)**: Envío de notificaciones por email.

- **Okta**: Autenticación y autorización de usuarios mediante SAML2 y SCIM.

## Funcionalidad Principal

- **Gestión de Presupuestos**: CRUD, jerarquía de áreas/departamentos, proveedores, categorías de gasto.
- **Notificaciones**: Emails automáticos por eventos clave usando AWS SES.
- **Salud y Debug**: Endpoints `/health`, `/api/userInfo`, `/api/debug/*` para monitoreo y pruebas.

## Endpoints Principales

- `/health`: Estado del servicio
- `/api/userInfo`: Información del usuario autenticado
- `/api/solicitudes-presupuesto`: CRUD de solicitudes
- `/api/areas`, `/api/departamentos`, `/api/subdepartamentos`: Gestión organizacional
- `/api/proveedores`, `/api/categorias-gasto`: Gestión de proveedores y categorías
- `/scim/v2/Users`: Endpoints SCIM para usuarios (Okta provisioning)
- `/scim/v2/Groups`: Endpoints SCIM para grupos (Okta provisioning)

## Ejemplo de Flujo de Trabajo

1. **Creación de Solicitud de Presupuesto**:
   - Un usuario crea una solicitud de presupuesto a través del endpoint `/api/solicitudes-presupuesto`.
   - El sistema guarda la solicitud en DynamoDB y envía una notificación por email al aprobador.

2. **Aprobación de Solicitud**:
   - El aprobador recibe un email y accede al sistema.
   - El aprobador revisa la solicitud y cambia el estatus a "Aprobado" o "Rechazado" mediante el endpoint correspondiente.
   - El sistema envía una notificación al solicitante con el resultado.

3. **Gestión de Proveedores y Categorías**:
   - Los administradores pueden gestionar proveedores y categorías de gasto a través de los endpoints `/api/proveedores` y `/api/categorias-gasto`.

## Estructura de Datos

### Usuario SCIM
```json
{
  "id": "string", // Este campo es obligatorio y debe estar presente en todos los usuarios
  "userName": "string",
  "name": {
    "given": "string",
    "family": "string"
  },
  "emails": [
    {
      "value": "string",
      "primary": true
    }
  ],
  "active": true,
  "roles": ["string"]
}
```

### Grupo SCIM
```json
{
  "id": "string",
  "displayName": "string",
  "members": [
    {
      "value": "string",
      "display": "string"
    }
  ]
}
```

### Solicitud de Presupuesto
```json
{
  "id": "string",
  "solicitudId": "string",
  "solicitante": "string",
  "numeroEmpleado": "string",
  "correo": "string",
  "cecos": "string",
  "departamento": "string",
  "subDepartamento": "string",
  "centroCostos": "string",
  "categoriaGasto": "string",
  "cuentaGastos": "string",
  "nombre": "string",
  "presupuestoDepartamento": "string",
  "presupuestoArea": "string",
  "montoSubtotal": 0,
  "estatusConfirmacion": "string",
  "fecha": "string",
  "periodoPresupuesto": "string",
  "empresa": "string",
  "proveedor": "string",
  "fechaCreacion": "2021-01-01T00:00:00Z",
  "fechaActualizacion": "2021-01-01T00:00:00Z",
  "creadoPor": "string",
  "actualizadoPor": "string",
  "archivosAdjuntos": ["string"],
  "comentarios": "string"
}
```

## Seguridad

### Autenticación via API Gateway y SCIM
- Headers de usuario válidos requeridos para todas las operaciones
- Endpoints `/scim/v2/**` protegidos para Okta provisioning (por ejemplo, Basic Auth)
- Verificación de roles para operaciones administrativas via `x-user-roles`

### Autorización
- **Admin**: Acceso completo a gestión y cambio de estatus
- **User**: Acceso a crear solicitudes y ver propias solicitudes

### Validaciones
- Validación de roles válidos
- Sanitización de datos de entrada
- Headers de contexto de usuario desde API Gateway

## Instalación y Ejecución

### Prerrequisitos
- Java 11 o superior
- Maven
- AWS CLI configurado

### Pasos de Instalación
1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/tu-usuario/presupuesto-backend.git
   cd presupuesto-backend
   ```

2. **Construir el proyecto**
   ```bash
   mvn clean package
   ```

3. **Crear tablas en DynamoDB**
   ```bash
   # Ejecutar scripts con prefijo de ambiente
   ENVIRONMENT=qa TEAM_PREFIX=fin ./dynamodb-scripts/create-solicitudes-presupuesto-aws.sh
   ENVIRONMENT=qa TEAM_PREFIX=fin ./dynamodb-scripts/create-proveedores-aws.sh
   ENVIRONMENT=qa TEAM_PREFIX=fin ./dynamodb-scripts/create-departamentos-aws.sh
   ENVIRONMENT=qa TEAM_PREFIX=fin ./dynamodb-scripts/create-categorias-gasto-aws.sh
   ENVIRONMENT=qa TEAM_PREFIX=fin ./dynamodb-scripts/create-scim-users-aws.sh
   ENVIRONMENT=qa TEAM_PREFIX=fin ./dynamodb-scripts/create-scim-groups-aws.sh
   # ... otros scripts según necesidad
   ```

4. **Configurar variables de entorno**
   - `ENVIRONMENT`: `dev`, `qa`, o `prod`
   - `SPRING_PROFILES_ACTIVE`: `lambda,{environment}`
   - `AWS_REGION`: Región de AWS (ej: `us-east-2`)

5. **Ejecutar la aplicación**
   ```bash
   java -jar target/presupuesto-backend.jar --spring.profiles.active=dev
   # Se ejecuta en http://localhost:8080 sin SSL
   ```

## Uso de Importación CSV

### 1. Descargar Template
```bash
curl -o template.csv http://localhost:8080/api/usuarios/csv-template
```

### 2. Preparar archivo CSV
```csv
email,nombre,numeroEmpleado,role,departamento,area,activo
juan.perez@empresa.com,Juan Pérez,EMP001,User,IT,Sistemas,true
admin@empresa.com,Administrador,EMP002,Admin,Administración,Finanzas,true
```

### 3. Importar usuarios
```bash
curl -X POST \
  -H "Authorization: Bearer your-jwt-token" \
  -F "file=@usuarios.csv" \
  http://localhost:8080/api/usuarios/import-csv
```

## Testing

- Pruebas unitarias: `mvn test`
- Pruebas de integración: `mvn verify`

## Estructura del Proyecto

```
presupuesto-backend
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── cdc
│   │   │           └── presupuesto
│   │   │               ├── controller
│   │   │               ├── model
│   │   │               ├── repository
│   │   │               └── service
│   │   └── resources
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── application-qa.yml
│   └── test
│       └── java
│           └── com
│               └── cdc
│                   └── presupuesto
└── pom.xml
```

## Notas Adicionales

- Asegúrate de tener las credenciales de AWS configuradas en tu entorno para poder acceder a los servicios de AWS.
- Revisa los scripts de creación de tablas en `dynamodb-scripts/` para más detalles sobre la estructura de las tablas.
- Para el despliegue en producción, sigue las guías específicas de AWS Lambda y API Gateway.

---

Este documento será actualizado conforme el sistema evoluciona y se añaden nuevas funcionalidades o se realizan cambios en la arquitectura.

---

### Paso a paso para usar SCIM Connection con Okta

1. **Verifica endpoints SCIM en el backend**  
   El backend expone los endpoints SCIM estándar bajo `/scim/v2/Users` y `/scim/v2/Groups` (ya implementados en `ScimController.java`).

2. **Configura el token de autenticación SCIM**  
   El backend espera un token Bearer en el header `Authorization`. El valor por defecto es:  
   `scim-2025-qa-SECRET-TOKEN-123456`  
   Puedes cambiarlo en la variable de entorno o en `application.properties` usando la propiedad `scim.token`.

3. **Configura la URL base SCIM en Okta**  
   La URL base para Okta debe ser:  
   ```
   https://<tu-dominio-backend>/scim/v2
   ```
   Ejemplo para QA:  
   ```
   https://api-qa.tuempresa.com/scim/v2
   ```

4. **Configura el token Bearer en Okta**  
   En Okta, en la sección de SCIM Connection, pon el token en el campo Authorization (solo el valor, sin "Bearer ").

5. **Configura los endpoints en Okta**  
   Okta detecta automáticamente los endpoints `/Users` y `/Groups` bajo la URL base.

6. **Configura los atributos requeridos**  
   Los atributos soportados por el backend están en el README y son compatibles con el estándar SCIM (ver ejemplos en README).

---

### Información para configurar en Okta

#### Configuración SCIM en Okta

- **SCIM Base URL:**  
  `https://<tu-dominio-backend>/scim/v2`

- **Autenticación:**  
  - Tipo: HTTP Header
  - Header: `Authorization`
  - Valor: `Bearer scim-2025-qa-SECRET-TOKEN-123456`  
    *(o el valor configurado en la propiedad `scim.token`)*

- **Atributos soportados para usuarios:**  
  - `userName`
  - `name.given`
  - `name.family`
  - `emails.value`
  - `active`
  - `roles`

- **Atributos soportados para grupos:**  
  - `displayName`
  - `members.value`
  - `members.display`

- **Endpoints:**  
  - `/scim/v2/Users`
  - `/scim/v2/Users/{id}`
  - `/scim/v2/Groups`
  - `/scim/v2/Groups/{id}`

- **Métodos soportados:**  
  - GET, POST, PUT, PATCH, DELETE

- **Token de prueba para QA:**  
  `scim-2025-qa-SECRET-TOKEN-123456`

- **Ejemplo de header:**  
  ```
  Authorization: Bearer scim-2025-qa-SECRET-TOKEN-123456
  ```

- **Notas:**  
  - El token puede cambiar por ambiente (`dev`, `qa`, `prod`).
  - El endpoint `/scim/v2` debe estar accesible desde Okta (firewall, CORS, etc).
  - El backend responde con los atributos SCIM estándar.

## Recomendaciones de mejora

- **Endpoints:** Documenta todos los endpoints públicos y protegidos, incluyendo `/usuario/saml-info` si lo agregas en la raíz.
- **SCIM:** Asegúrate que todos los objetos usuario SCIM incluyan el atributo `"id"` y que los métodos de listado y creación lo agreguen si falta.
- **SAML2:** Usa siempre los valores de entityId, SSO URL y certificado desde AWS Parameter Store o properties, nunca hardcode.
- **Seguridad:** Revisa que los endpoints realmente públicos estén permitidos en SecurityConfig y que los protegidos usen roles y/o tokens.
- **CORS:** Valida que los orígenes permitidos sean los mínimos necesarios para QA y producción.
- **Logging:** Usa niveles de logging apropiados por ambiente (`DEBUG` en dev/qa, `INFO`/`WARN` en prod).
- **Pruebas:** Agrega ejemplos de pruebas con `curl` para cada endpoint principal.
- **DynamoDB:** Documenta los índices secundarios y las claves de cada tabla en DYNAMODB.md.
- **Variables de entorno:** Documenta todas las variables requeridas y opcionales en ENVIRONMENT-CONFIGURATION.md.
- **Manejo de errores:** Asegúrate que todos los endpoints devuelvan mensajes claros y estructurados en caso de error.
- **Despliegue:** Documenta el proceso de despliegue en Lambda y la integración con API Gateway.
- **Dependencias:** Revisa y elimina dependencias duplicadas en el pom.xml (por ejemplo, versiones repetidas de spring-security-saml2-service-provider).

- **Stage:** El valor del stage (`/dev`, `/qa`, `/prod`) se define en los archivos de configuración y se usa en rutas, redirecciones y seguridad para asegurar que los endpoints y redirecciones sean correctos según el ambiente.
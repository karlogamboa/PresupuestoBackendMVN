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

- El backend acepta y procesa cookies de sesión SAML si la sesión está habilitada (`SessionCreationPolicy.IF_REQUIRED`).
- Asegúrate que el navegador envíe las cookies en las solicitudes a endpoints protegidos.
- La configuración de CORS permite credenciales (`allowCredentials=true`) y todos los headers/métodos necesarios.
- Los endpoints protegidos requieren autenticación y validan los permisos del usuario.
- Los headers de autenticación y usuario deben estar presentes y correctos en las solicitudes.

---

## Verificación de Cookies de Sesión SAML y Headers

El backend está configurado para aceptar y procesar cookies de sesión SAML.  
Asegúrate de que el navegador envíe las cookies en las solicitudes a endpoints protegidos (`/api/**`, `/saml/user`, etc).

- **CORS:** La configuración permite credenciales y todos los headers/métodos necesarios.
- **Autenticación:** Los endpoints protegidos requieren que el usuario esté autenticado (cookie de sesión SAML válida).
- **Permisos:** El backend valida los roles del usuario antes de permitir acceso a endpoints protegidos.
- **Headers:** Los headers de autenticación y usuario (`Authorization`, `x-user-id`, `x-user-roles`, etc) deben estar presentes y correctos en las solicitudes.

Para debug, revisa los logs del backend para confirmar que las cookies de sesión SAML y los headers se reciben correctamente.

---

## Instalación y Ejecución

### Prerrequisitos
- Java 11 o superior
- Maven
- AWS CLI configurado

> **Nota de solución de problemas:**  
> Si ves el error de compilación `package javax.servlet.http does not exist`, asegúrate de que tu proyecto incluya la dependencia de `javax.servlet-api` en el archivo `pom.xml`.  
> Ejemplo de dependencia Maven:
> ```xml
> <dependency>
>   <groupId>javax.servlet</groupId>
>   <artifactId>javax.servlet-api</artifactId>
>   <version>4.0.1</version>
>   <scope>provided</scope>
> </dependency>
> ```
> Si usas Spring Boot, normalmente esta dependencia ya está incluida, pero si usas un entorno diferente o excluiste dependencias, agrégala manualmente.

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
  `https://v9hhsb7ju3.execute-api.us-east-2.amazonaws.com/qa/scim/v2`

- **Unique identifier field for users:**  
  `id`
  > El campo `id` es el identificador único para usuarios en DynamoDB y en el modelo SCIM.  
  > Debe configurarse como el "Unique identifier field for users" en la conexión SCIM de Okta.

- **Autenticación:**  
  - Tipo: HTTP Header
  - Header: `Authorization`
  - Valor: `Bearer scim-2025-qa-BEARER-2f8c1e7a4b7d4e8c9a1f6b3c2d5e7f8a`  
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

## Ejemplo de consulta de usuarios SCIM con curl

```sh
# curl para listar usuarios SCIM con el token configurado
curl -X GET "https://v9hhsb7ju3.execute-api.us-east-2.amazonaws.com/qa/scim/v2/Users" \
  -H "Authorization: Bearer scim-2025-qa-BEARER-2f8c1e7a4b7d4e8c9a1f6b3c2d5e7f8a"
```

## Ejemplo de creación de usuario SCIM con curl

```sh
curl -X POST "https://v9hhsb7ju3.execute-api.us-east-2.amazonaws.com/qa/scim/v2/Users" \
  -H "Authorization: Bearer scim-2025-qa-BEARER-2f8c1e7a4b7d4e8c9a1f6b3c2d5e7f8a" \
  -H "Content-Type: application/json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "karlo+cdc@zicral.com",
    "name": {
      "givenName": "Karlo",
      "familyName": "Gamboa"
    },
    "emails": [{
      "value": "karlo+cdc@zicral.com",
      "primary": true
    }],
    "active": true
  }'
```

## Arquitectura Serverless y SAML2

Este backend está diseñado para ejecutarse en AWS Lambda detrás de API Gateway, usando DynamoDB y SES, y autenticación SAML2 con Okta.

- **Patrón Lambda/API Gateway:**  
  - Todas las rutas HTTP son gestionadas por API Gateway y delegadas a Lambda.
  - El backend es stateless, optimizado para cold start y escalabilidad.
  - La configuración sensible (secrets, endpoints, certificados) se obtiene de AWS Parameter Store o Secrets Manager.

- **Integración SAML2 con Okta:**  
  - El flujo SAML2 inicia en Okta y regresa al endpoint ACS en Lambda/API Gateway.
  - El ACS (`/login/saml2/sso/okta-saml`) valida la respuesta SAML y genera un JWT para el frontend.
  - El Audience Restriction (SP Entity ID) en Okta debe ser igual al ACS URL configurado en el backend.
  - El backend expone `/saml/user` para obtener atributos SAML del usuario autenticado.

- **DynamoDB:**  
  - Todas las entidades principales usan DynamoDB con prefijos por ambiente.
  - Los índices secundarios permiten búsquedas eficientes (por ejemplo, por número de empleado).

- **SCIM y Okta:**  
  - Endpoints SCIM para aprovisionamiento de usuarios y grupos desde Okta.
  - Token SCIM configurable por ambiente.

- **CORS y Seguridad:**  
  - CORS configurado para permitir solo los orígenes necesarios (frontend y Okta).
  - Cookies de sesión SAML y JWT son aceptadas y procesadas por Lambda.

## Flujo SAML2 Serverless

1. El usuario inicia sesión en Okta y es redirigido al ACS en API Gateway/Lambda.
2. Lambda procesa la respuesta SAML, valida el Audience y genera un JWT.
3. El usuario es redirigido al frontend con el JWT como parámetro o cookie.
4. El backend valida el JWT en cada petición protegida.


# Sistema de Gestión de Presupuestos - Backend

Backend moderno para la gestión de presupuestos, desarrollado en **Spring Boot** y optimizado para **AWS Lambda**. Incluye integración con **DynamoDB**, autenticación y autorización vía **API Gateway Authorizer**, y notificaciones automáticas por **AWS SES**. El sistema está preparado para ambientes multi-entorno y sigue buenas prácticas de seguridad, configuración y despliegue serverless.

## Cambios recientes (julio 2025)
- Eliminado todo rastro de Spring Mail y Swagger/OpenAPI.
- CORS global y seguro, configurable por Parameter Store.
- Perfiles Spring: `lambda`, `dev`, `qa`, `prod`, y `!lambda` (local).
- Email solo por AWS SES (bean `SesClient`).
- Seguridad y roles solo por API Gateway Authorizer.
- Documentación y scripts legacy movidos a archivos separados.

## Arquitectura y Componentes
...existing code...

## Arquitectura y Componentes

- **Spring Boot**: Framework principal, desacoplado de Tomcat para ejecución serverless.
- **AWS Lambda**: Ejecución serverless, cold start optimizado, sin estado.
- **API Gateway**: Expone endpoints REST, maneja autenticación y roles vía headers personalizados (`x-user-id`, `x-user-email`, `x-user-roles`).
- **DynamoDB**: Base de datos NoSQL, con prefijos de tabla por ambiente (`dev`, `qa`, `prod`, `lambda`).
- **AWS Parameter Store**: Configuración dinámica de CORS y otros parámetros sensibles.
- **AWS SES**: Envío de notificaciones por email, centralizado en `EmailService`.

## Perfiles y Configuración

- **Perfiles Spring**: `lambda`, `dev`, `qa`, `prod`, y `!lambda` (local). Cada uno ajusta endpoints, CORS, y credenciales.
- **CORS**: Filtro global (`GlobalCorsHeaderFilterConfig`) asegura headers correctos y personalizados en todas las respuestas, con valores obtenidos de Parameter Store.
- **Seguridad**: Autenticación y autorización gestionadas por API Gateway. El backend valida y expone el contexto de usuario y roles.
- **Logging**: Configuración robusta para ambientes Lambda y locales.

## Funcionalidad Principal

- **Gestión de Solicitantes**: CRUD, importación masiva por CSV, validaciones de unicidad y consistencia.
- **Gestión de Presupuestos**: CRUD, jerarquía de áreas/departamentos, proveedores, categorías de gasto.
- **Notificaciones**: Emails automáticos por eventos clave usando AWS SES.
- **Salud y Debug**: Endpoints `/health`, `/api/userInfo`, `/api/debug/*` para monitoreo y pruebas.

## Integraciones AWS

- **DynamoDB**: Acceso desacoplado por repositorios, uso de prefijos de tabla por ambiente.
- **SES**: Toda la lógica de email centralizada en `EmailService`, sin dependencias de Spring Mail.
- **Parameter Store**: Uso recomendado para orígenes CORS, credenciales y parámetros sensibles.

## Limpieza y Buenas Prácticas

- **Swagger/OpenAPI**: Eliminado del proyecto para reducir superficie de ataque y dependencias innecesarias.
- **Spring Mail**: Eliminado; toda la mensajería usa AWS SES.
- **Filtros y Seguridad**: Filtros de CORS y autenticación implementados como `OncePerRequestFilter` y configurados globalmente.
- **Validación y Refactorización**: Controladores revisados para evitar duplicación y mejorar validación con anotaciones.

## Despliegue y Ejecución

1. **Compilar**: `mvn clean package -DskipTests`
2. **Desplegar**: Subir el JAR generado a AWS Lambda (handler: `com.cdc.presupuesto.lambda.LambdaHandler`)
3. **Configurar**: Ajustar variables de entorno y Parameter Store para cada ambiente (`aws.region`, `aws.dynamodb.table.prefix`, CORS, etc.)

## Endpoints Principales

- `/health`: Estado del servicio
- `/api/userInfo`: Información del usuario autenticado
- `/api/solicitudes-presupuesto`: CRUD de solicitudes
- `/api/solicitantes`: CRUD de solicitantes
- `/api/areas`, `/api/departamentos`, `/api/subdepartamentos`: Gestión organizacional
- `/api/proveedores`, `/api/categorias-gasto`: Gestión de proveedores y categorías

## Notas y Recomendaciones

- **Swagger y Spring Mail**: Eliminados completamente. No agregar dependencias ni configuraciones relacionadas.
- **CORS**: Configurado globalmente, personalizable por Parameter Store.
- **Perfiles**: Usar el perfil adecuado para cada entorno (`lambda` para producción Lambda, `!lambda` para local).
- **Documentación**: Mantener este README actualizado ante cambios de arquitectura o dependencias.

---

_Última actualización: julio 2025_
- **Enhanced Client**: DynamoDB Enhanced Client para operaciones optimizadas
- **Parameter Store**: Configuración centralizada y dinámica

## Estructura del Proyecto

```
src/main/java/com/cdc/presupuesto/
├── config/           # Configuración de Spring Boot y AWS
├── controller/       # Controladores REST
├── model/           # Entidades/Modelos de datos
├── repository/      # Repositorios para acceso a datos
├── service/         # Lógica de negocio
└── PresupuestoBackendApplication.java
```

## Endpoints Principales

### Autenticación y Configuración
- `POST /api/userInfo` - Información del usuario autenticado via API Gateway
- `POST /api/debug/auth-info` - Debug de información de autenticación
- `GET /api/auth-config` - Configuración de autenticación
- `POST /api/logout` - Logout (retorna confirmación)

### Gestión de Solicitantes
- `GET /api/solicitantes` - Listar todos los solicitantes
- `POST /api/solicitantes` - Crear nuevo solicitante
- `PUT /api/solicitantes/{id}` - Actualizar solicitante
- `DELETE /api/solicitantes/{id}` - Eliminar solicitante
- `POST /api/solicitantes/import-csv` - Importar solicitantes desde CSV

### Gestión de Solicitudes de Presupuesto
- `GET /api/solicitudes-presupuesto` - Listar solicitudes
- `POST /api/solicitudes-presupuesto` - Crear nueva solicitud
- `PUT /api/solicitudes-presupuesto/{id}` - Actualizar solicitud
- `DELETE /api/solicitudes-presupuesto/{id}` - Eliminar solicitud

### Gestión Organizacional
- `GET /api/areas` - Listar áreas
- `GET /api/departamentos` - Listar departamentos
- `GET /api/subdepartamentos` - Listar subdepartamentos (filtrable por área)
- `GET /api/proveedores` - Listar proveedores
- `GET /api/categorias-gasto` - Listar categorías de gasto

### Resultados y Administración
- `GET /api/resultados` - Obtener resultados (filtrable por empleado)
- `POST /api/resultados/editar-estatus` - Cambiar estatus (solo Admin)

### Email Notifications
- `POST /api/emails/send` - Enviar email genérico
- `POST /api/emails/send-budget-notification` - Notificación de presupuesto
- `POST /api/emails/send-status-notification` - Notificación de cambio de estatus

### Utilidades
- `GET /health` - Health check
- `GET /v3/api-docs` - Documentación OpenAPI
- `GET /swagger-ui.html` - Interfaz Swagger UI

## Configuración de Ambientes

### Variables de Entorno para Lambda
```properties
# Ambiente (dev/qa/prod)
ENVIRONMENT=qa
SPRING_PROFILES_ACTIVE=lambda,qa
AWS_REGION=us-east-2

# Configuración automática via Parameter Store
# /fin/{ENVIRONMENT}/presupuesto/...
```

### Configuración por Parameter Store
El sistema usa AWS Parameter Store para configuración dinámica:

```
/fin/qa/presupuesto/aws/region
/fin/qa/presupuesto/aws/dynamodb/table/prefix
/fin/qa/presupuesto/cors/allowed-origins
/fin/qa/presupuesto/security/auth/enabled
# ... más parámetros según ENVIRONMENT-CONFIGURATION.md
```

## Instalación y Ejecución

### Prerrequisitos
- Java 17+
- Maven 3.6+
- Cuenta AWS con acceso a DynamoDB y SES
- AWS CLI configurado
- **Para Lambda**: AWS SAM CLI (recomendado) o AWS CLI

### Pasos de Despliegue

1. **Clonar el repositorio**
   ```bash
   git clone <repository-url>
   cd PresupuestoBackendMVN
   ```

2. **Configurar Parameter Store**
   ```bash
   # Configurar parámetros por ambiente
   ENVIRONMENT=qa ./aws-scripts/create-parameter-store.sh
   # o en Windows
   set ENVIRONMENT=qa && aws-scripts\create-parameter-store.bat
   ```

3. **Crear tablas en DynamoDB**
   ```bash
   # Ejecutar scripts con prefijo de ambiente
   ENVIRONMENT=qa TEAM_PREFIX=fin ./dynamodb-scripts/create-solicitudes-presupuesto-aws.sh
   ENVIRONMENT=qa TEAM_PREFIX=fin ./dynamodb-scripts/create-solicitantes-aws.sh
   # ... otros scripts según necesidad
   ```

4. **Compilar y Desplegar en Lambda**
   ```bash
   # Compilar para Lambda
   mvn clean package
   
   # Desplegar función Lambda (configurar según tu setup)
   aws lambda update-function-code --function-name presupuesto-backend-qa \
     --zip-file fileb://target/presupuesto-backend-1.0.0-SNAPSHOT-aws.jar
   ```

### Desarrollo Local (Opcional)
Para desarrollo y pruebas locales:
```bash
# Perfil de desarrollo sin autenticación
java -jar target/presupuesto-backend.jar --spring.profiles.active=dev
# Acceder a http://localhost:8080
```
   ```

   **Opción C: Helper Interactivo**
   ```bash
   # Linux/Mac
   ./aws-scripts/layer-helper.sh
   
   # Windows
   aws-scripts\layer-helper.bat
   ```

   **Opción D: AWS Lambda con SAM**
   ```bash
   # Instalar SAM CLI
   # https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html
   
   chmod +x deploy-sam.sh
   ./deploy-sam.sh
   ```

5. **Verificar instalación**
   ```bash
   # Verificar health check en API Gateway
   curl https://your-api-gateway-url/health
   
   # Ver logs de Lambda
   aws logs tail /aws/lambda/presupuesto-backend-qa --follow
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

### Ejecutar tests
```bash
mvn test
```

### Test de importación CSV
```bash
# Usar el archivo sample_usuarios.csv incluido
curl -X POST \
  -H "Authorization: Bearer your-admin-token" \
  -F "file=@sample_usuarios.csv" \
  http://localhost:8080/api/usuarios/import-csv
```

## Estructura de Datos

### Usuario
```json
{
  "id": "uuid",
  "email": "usuario@empresa.com",
  "nombre": "Nombre Completo",
  "numeroEmpleado": "EMP001",
  "role": "User|Admin",
  "departamento": "IT",
  "area": "Sistemas",
  "activo": "true",
  "fechaCreacion": "2025-01-01 10:00:00",
  "fechaActualizacion": "2025-01-01 10:00:00"
}
```

### Solicitud de Presupuesto
```json
{
  "id": "uuid",
  "numEmpleado": "EMP001",
  "fechaSolicitud": "2025-01-01",
  "area": "IT",
  "departamento": "Sistemas",
  "subdepartamento": "Desarrollo",
  "proveedor": "Proveedor XYZ",
  "categoriaGasto": "Software",
  "monto": 1000.00,
  "estatus": "Pendiente",
  "comentarios": "Comentarios adicionales"
}
```

## Seguridad

### Autenticación via API Gateway
- Headers de usuario válidos requeridos para todas las operaciones
- Verificación de roles para operaciones administrativas via `x-user-roles`

### Autorización
- **Admin**: Acceso completo a gestión y cambio de estatus
- **User**: Acceso a crear solicitudes y ver propias solicitudes

### Validaciones
- Validación de emails únicos en solicitantes
- Validación de roles válidos
- Sanitización de datos de entrada
- Headers de contexto de usuario desde API Gateway

## Monitoreo y Logs

### Health Check
```bash
# En API Gateway URL
curl https://your-api-gateway-url/health

# Para desarrollo local
curl http://localhost:8080/health
```

### Testing y Validación
```bash
# Verificar health check
curl https://your-api-gateway-url/health

# Ver logs en CloudWatch
aws logs tail /aws/lambda/presupuesto-backend-qa --follow

# Probar endpoints
curl -X POST https://your-api-gateway-url/api/userInfo \
  -H "x-user-id: test@example.com" \
  -H "x-user-roles: Admin"
```

### Logs y Monitoreo
- **CloudWatch Logs**: `/aws/lambda/presupuesto-backend-{environment}`
- **Metrics**: Duración, errores, cold starts automáticamente en CloudWatch
- **X-Ray**: Tracing distribuido (opcional, configurable)
- **API Gateway Logs**: Request/response logging configurado por ambiente

## Arquitectura Lambda-First

### Beneficios del Diseño Serverless
- **Costo**: Solo pagas por requests ejecutados
- **Escalabilidad**: Automática según demanda
- **Mantenimiento**: Sin administración de servidores
- **Seguridad**: Aislamiento por ejecución
- **Disponibilidad**: Alta disponibilidad multi-AZ automática

### Optimizaciones Implementadas
- **Cold Start**: Lazy initialization y configuración mínima
- **Memory**: Configurado para balance costo/performance
- **Timeout**: Ajustado según tipo de operación
- **Environment**: Variables y Parameter Store para configuración dinámica

### Configuración de Ambientes

### Configuración de Ambientes
Para más detalles sobre configuración específica por ambiente, consultar:
- **ENVIRONMENT-CONFIGURATION.md**: Configuración completa por ambiente
- **Parameter Store**: Configuración dinámica centralizada
- **DynamoDB Scripts**: Scripts de creación de tablas con prefijos

### Seguridad y Autenticación
- **API Gateway Authorizer**: Autenticación centralizada
- **Roles**: Admin/User manejados via headers
- **Stateless**: Diseño completamente sin estado para Lambda
- **CORS**: Configurado dinámicamente por ambiente

## Contribuir

1. Fork el proyecto
2. Crear una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear un Pull Request

## Documentación Adicional

- **ENVIRONMENT-CONFIGURATION.md**: Configuración detallada por ambiente
- **AWS Parameter Store**: Configuración de parámetros
- **deploy-scripts/README-LEGACY.md**: Scripts legacy (no usar)

## Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo LICENSE para detalles.

## Soporte

Para soporte técnico o preguntas, contactar a:
- Email: soporte@empresa.com
- Documentación: Ver documentación específica en archivos MD del proyecto

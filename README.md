# Sistema de Gestión de Presupuestos - Backend

Este es el backend del sistema de gestión de presupuestos desarrollado con Spring Boot, que incluye integración con AWS DynamoDB, autenticación OAuth2 con Okta, y funcionalidad completa de gestión de usuarios con importación CSV.

## Características Principales

### 🔐 Autenticación y Autorización
- **OAuth2/JWT**: Integración con Okta para autenticación
- **Roles**: Sistema de roles Admin/User
- **Información de usuarios**: Almacenada en DynamoDB (no en JWT)

### 👥 Gestión de Usuarios
- **CRUD completo**: Crear, leer, actualizar, eliminar usuarios
- **Importación CSV**: Importación masiva de usuarios desde archivos CSV
- **Validaciones**: Email único, roles válidos, campos requeridos
- **Template CSV**: Descarga de template para importación

### 💰 Gestión de Presupuestos
- **Solicitudes**: Crear y gestionar solicitudes de presupuesto
- **Áreas y Departamentos**: Gestión jerárquica de estructura organizacional
- **Proveedores**: Gestión de proveedores y categorías de gasto
- **Notificaciones**: Envío automático de emails via Amazon SES

### 📊 Base de Datos
- **DynamoDB**: Almacenamiento NoSQL en AWS
- **Tablas**: usuarios, solicitudes-presupuesto, areas, departamentos, etc.
- **Enhanced Client**: Uso de DynamoDB Enhanced Client para operaciones

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

### Autenticación
- `POST /api/userInfo` - Información del usuario autenticado
- `POST /api/exchange-token` - Intercambio de tokens OAuth2
- `POST /api/logout` - Cerrar sesión
- `GET /api/okta-config` - Configuración de Okta

### Gestión de Usuarios
- `GET /api/usuarios` - Listar todos los usuarios (Admin)
- `GET /api/usuarios/{id}` - Obtener usuario por ID
- `POST /api/usuarios` - Crear nuevo usuario (Admin)
- `PUT /api/usuarios/{id}` - Actualizar usuario (Admin)
- `DELETE /api/usuarios/{id}` - Eliminar usuario (Admin)
- `POST /api/usuarios/import-csv` - Importar usuarios desde CSV (Admin)
- `GET /api/usuarios/csv-template` - Descargar template CSV

### Solicitudes de Presupuesto
- `GET /api/solicitudes-presupuesto` - Listar solicitudes
- `POST /api/solicitudes-presupuesto` - Crear nueva solicitud
- `PUT /api/solicitudes-presupuesto/{id}` - Actualizar solicitud
- `DELETE /api/solicitudes-presupuesto/{id}` - Eliminar solicitud

### Otros
- `GET /api/areas` - Gestión de áreas
- `GET /api/departamentos` - Gestión de departamentos
- `GET /api/proveedores` - Gestión de proveedores
- `GET /api/categorias-gasto` - Categorías de gasto
- `GET /health` - Health check

## Configuración

### Variables de Entorno
```properties
# AWS
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key

# DynamoDB
aws.dynamodb.table-prefix=presupuesto-

# Okta
okta.oauth2.issuer=https://your-domain.okta.com/oauth2/default
okta.oauth2.client-id=your-client-id
okta.oauth2.client-secret=your-client-secret
okta.oauth2.audience=api://default

# CORS
cors.allowed-origins=http://localhost:3000,https://your-frontend.com

# Email
spring.mail.host=email-smtp.us-east-1.amazonaws.com
spring.mail.port=587
spring.mail.username=your-smtp-username
spring.mail.password=your-smtp-password
```

## Instalación y Ejecución

### Prerrequisitos
- Java 17+
- Maven 3.6+
- Cuenta AWS con acceso a DynamoDB y SES
- Cuenta Okta configurada
- **Para Lambda**: AWS SAM CLI (recomendado) o AWS CLI

### Pasos
1. **Clonar el repositorio**
   ```bash
   git clone <repository-url>
   cd PresupuestoBackendMVN
   ```

2. **Configurar variables de entorno**
   ```bash
   # Editar application.properties o usar variables de entorno
   ```

3. **Crear tablas en DynamoDB**
   ```bash
   # Ejecutar scripts en dynamodb-scripts/
   chmod +x dynamodb-scripts/*.sh
   ./dynamodb-scripts/create-usuarios-aws.sh
   ./dynamodb-scripts/create-solicitudes-presupuesto-aws.sh
   # ... otros scripts
   ```

4. **Deployment - Elegir una opción:**

   **Opción A: EC2 Tradicional**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

   **Opción B: AWS Lambda con SAM (Recomendado)**
   ```bash
   # Instalar SAM CLI
   # https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html
   
   chmod +x deploy-sam.sh
   ./deploy-sam.sh
   ```

   **Opción C: AWS Lambda Manual**
   ```bash
   chmod +x deploy-lambda.sh
   ./deploy-lambda.sh
   ```

5. **Verificar instalación**
   
   **EC2:**
   ```bash
   curl https://localhost:8443/health
   ```
   
   **Lambda:**
   ```bash
   # URL será proporcionada después del deployment
   curl https://your-api-gateway-url.execute-api.us-east-2.amazonaws.com/dev/health
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

### Autenticación
- JWT tokens válidos requeridos para todas las operaciones
- Verificación de roles para operaciones administrativas

### Autorización
- **Admin**: Acceso completo a gestión de usuarios y cambio de estatus
- **User**: Acceso a crear solicitudes y ver propias solicitudes

### Validaciones
- Validación de emails únicos
- Validación de roles válidos
- Sanitización de datos de entrada

## Monitoreo y Logs

### Health Check
```bash
curl http://localhost:8080/health
```

### Logs
- Logs detallados de todas las operaciones
- Información de importaciones CSV
- Errores y warnings para debugging

## Deployment

### Desarrollo Local
```bash
mvn spring-boot:run
```

### EC2 Tradicional
```bash
mvn clean package
java -jar target/presupuesto-backend-1.0.0-SNAPSHOT.jar
```

### AWS Lambda con SAM CLI (Recomendado)
```bash
# Instalar SAM CLI primero
# https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html

# Deployment automático
./deploy-sam.sh        # Linux/macOS
deploy-sam.bat         # Windows
```

### AWS Lambda Manual
```bash
# Compilar para Lambda
mvn clean package

# Deployment manual
./deploy-lambda.sh     # Linux/macOS
deploy-lambda.bat      # Windows
```

### Docker (opcional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/presupuesto-backend-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Arquitectura Multi-Entorno

### EC2 vs Lambda
El sistema está diseñado para funcionar tanto en:

**EC2 (Tradicional)**
- Servidor siempre activo
- Puerto 8443 (HTTPS)
- Configuración en `application.properties`
- Perfil por defecto

**AWS Lambda (Serverless)**
- Ejecución bajo demanda
- API Gateway como frontend
- Configuración en `application-lambda.properties`
- Perfil `lambda`
- Cold start optimizado

### Configuración por Entorno

**EC2:**
```properties
# application.properties
server.port=8443
server.ssl.enabled=true
spring.profiles.active=default
```

**Lambda:**
```properties
# application-lambda.properties
spring.main.lazy-initialization=true
spring.profiles.active=lambda
server.ssl.enabled=false
```

### Comparación EC2 vs Lambda

| Característica | EC2 | Lambda |
|---|---|---|
| **Costo** | Fijo (siempre corriendo) | Por uso (pay-per-request) |
| **Escalabilidad** | Manual/Auto Scaling Groups | Automática |
| **Cold Start** | No | Sí (3-5 segundos) |
| **Tiempo máximo ejecución** | Ilimitado | 15 minutos |
| **Gestión infraestructura** | Requiere administración | Sin administración |
| **SSL/TLS** | Configuración manual | Automático via API Gateway |
| **Logs** | CloudWatch + configuración | CloudWatch automático |
| **Ideal para** | Tráfico constante | Tráfico variable/esporádico |

### Recomendaciones de Uso

**Usar EC2 cuando:**
- Tráfico constante y predecible
- Necesitas control total del servidor
- Aplicaciones que requieren estado persistente
- Procesos de larga duración

**Usar Lambda cuando:**
- Tráfico variable o esporádico
- Quieres minimizar costos operacionales
- Escalabilidad automática es crítica
- Enfoque serverless/microservicios

## Contribuir

1. Fork el proyecto
2. Crear una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear un Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo LICENSE para detalles.

## Soporte

Para soporte técnico o preguntas, contactar a:
- Email: soporte@empresa.com
- Documentación: Ver `CSV_IMPORT_DOCUMENTATION.md` para detalles específicos de importación CSV

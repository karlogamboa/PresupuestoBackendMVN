# Authentication Modes - Modos de Autenticación

Este sistema puede operar en diferentes modos de autenticación dependiendo de la configuración y el perfil activo.

## 🔓 Modo Desarrollo Local (Sin Autenticación)

**Cuándo se activa:**
- Cuando NO se usa el perfil `lambda` (perfil por defecto)
- Todas las APIs son de acceso libre para facilitar el desarrollo

**Configuración:**
```properties
# No usar perfil lambda
spring.profiles.active=dev
```

**Seguridad OpenAPI:** No se muestra esquema JWT en la documentación

---

## 🔐 Modo Lambda Producción (Con Autenticación)

**Cuándo se activa:**
- Cuando se usa el perfil `lambda`
- `security.auth.enabled=true` (valor por defecto)

**Configuración:**
```properties
# Activar perfil lambda
spring.profiles.active=lambda

# Autenticación habilitada (por defecto)
security.auth.enabled=true
```

**Características:**
- Todas las APIs requieren autenticación vía API Gateway Authorizer
- Los headers `x-user-id`, `x-user-roles`, `x-user-email` son requeridos
- Filtro `ApiGatewayAuthenticationFilter` activo

**Seguridad OpenAPI:** Se muestra esquema JWT en la documentación

---

## ⚠️ Modo Lambda Pruebas (Sin Autenticación)

**Cuándo se activa:**
- Cuando se usa el perfil `lambda` 
- `security.auth.enabled=false` (configuración manual)

**Configuración:**
```properties
# Activar perfil lambda
spring.profiles.active=lambda

# Deshabilitar autenticación para pruebas
security.auth.enabled=false
```

**Características:**
- Todas las APIs son de acceso libre (para pruebas en Lambda)
- No se requieren headers de autenticación
- Filtro `ApiGatewayAuthenticationFilter` desactivado

**Seguridad OpenAPI:** No se muestra esquema JWT en la documentación

---

## 📝 Variables de Entorno

### Para desarrollo local:
```bash
# Sin variables especiales requeridas
```

### Para Lambda con autenticación:
```bash
export SPRING_PROFILES_ACTIVE=lambda
export security.auth.enabled=true
```

### Para Lambda sin autenticación (pruebas):
```bash
export SPRING_PROFILES_ACTIVE=lambda
export security.auth.enabled=false
```

---

## 🚀 Despliegue

### Desarrollo Local
```bash
# Ejecutar sin perfil lambda (modo desarrollo)
mvn spring-boot:run
```

### Lambda Producción
```bash
# Compilar con perfil lambda
mvn clean package -Paws-lambda

# Configurar variables de entorno en AWS Lambda:
# SPRING_PROFILES_ACTIVE=lambda
# security.auth.enabled=true
```

### Lambda Pruebas
```bash
# Compilar con perfil lambda
mvn clean package -Paws-lambda

# Configurar variables de entorno en AWS Lambda:
# SPRING_PROFILES_ACTIVE=lambda
# security.auth.enabled=false
```

---

## 🔍 Verificación del Modo Activo

La documentación OpenAPI mostrará automáticamente el modo activo:

- **🔓 MODO DESARROLLO**: Sin autenticación requerida en desarrollo local
- **🔐 MODO PRODUCCIÓN**: Autenticación requerida vía API Gateway Authorizer  
- **⚠️ MODO DE PRUEBA**: Autenticación deshabilitada. Todas las APIs son de acceso libre

---

## 🛡️ Seguridad por Configuración

| Perfil | security.auth.enabled | Comportamiento | OpenAPI JWT |
|--------|----------------------|----------------|-------------|
| `dev` (defecto) | cualquier valor | Sin autenticación | ❌ No |
| `lambda` | `true` (defecto) | Con autenticación | ✅ Sí |
| `lambda` | `false` | Sin autenticación | ❌ No |

---

## ⚡ Migración desde Okta/JWT

El sistema ha sido completamente migrado desde Okta/JWT a API Gateway Authorizer:

- ❌ **Eliminado**: Usuario entity, JWT tokens, Okta integration
- ✅ **Agregado**: API Gateway Authorizer support, configuración flexible
- ✅ **Mantenido**: Toda la funcionalidad de negocio, compatibilidad de APIs

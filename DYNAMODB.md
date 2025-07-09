# Uso de DynamoDB en PresupuestoBackendMVN

Este documento describe cómo se utiliza Amazon DynamoDB en el backend de presupuestos, incluyendo la estructura de tablas, mejores prácticas y recomendaciones para desarrollo y producción.

---

## 📚 Índice
- [Visión General](#visión-general)
- [Estructura de Tablas](#estructura-de-tablas)
- [Nombres y Convenciones](#nombres-y-convenciones)
- [Configuración por Ambiente](#configuración-por-ambiente)
- [Acceso desde el Backend](#acceso-desde-el-backend)
- [Buenas Prácticas](#buenas-prácticas)
- [Recomendaciones de Seguridad](#recomendaciones-de-seguridad)
- [Recursos Útiles](#recursos-útiles)

---

## Visión General

El backend utiliza DynamoDB como base de datos NoSQL principal para almacenar solicitudes de presupuesto y otros datos relacionados. Todas las operaciones CRUD se realizan a través del SDK de AWS para Java, y la configuración de tablas es dinámica según el ambiente (dev, qa, prod).

## Estructura de Tablas

Por defecto, cada ambiente tiene su propio prefijo de tabla. Ejemplo de tablas principales:

- **Solicitudes de Presupuesto**: `fin-dynamodb-<env>-presupuesto-solicitudes`
- (Agregar otras tablas si existen)

### Ejemplo de Estructura: `solicitudes`
| Atributo         | Tipo      | Descripción                        |
|------------------|-----------|------------------------------------|
| `id`             | String    | Clave primaria (PK)                |
| `fechaCreacion`  | String    | Fecha de creación (ISO8601)        |
| `usuarioId`      | String    | ID del usuario solicitante         |
| `estado`         | String    | Estado de la solicitud             |
| ...              | ...       | Otros atributos relevantes         |

> **Nota:** La estructura exacta puede variar según la evolución del modelo de negocio. Consultar el código fuente para detalles actualizados.

## Nombres y Convenciones

- **Prefijo:** `fin-dynamodb-<env>-presupuesto-`
  - `<env>` puede ser `dev`, `qa` o `prod`.
- **Separación por ambiente:** Cada ambiente tiene sus propias tablas para evitar colisiones y facilitar pruebas.
- **Variables de entorno:** El prefijo puede ser sobrescrito usando la variable `TABLE_PREFIX`.

## Configuración por Ambiente

- **Desarrollo:** `fin-dynamodb-dev-presupuesto-*`
- **QA:**        `fin-dynamodb-qa-presupuesto-*`
- **Producción:**`fin-dynamodb-prod-presupuesto-*`

La selección del prefijo es automática según la variable de entorno `ENVIRONMENT` y/o `TABLE_PREFIX`.

## Acceso desde el Backend

- El acceso a DynamoDB se realiza usando el AWS SDK v2 para Java.
- Las credenciales y la región se obtienen automáticamente desde el entorno Lambda.
- El nombre de la tabla se construye dinámicamente según el ambiente.
- La configuración sensible (por ejemplo, throughput, índices secundarios) se gestiona desde AWS Console o IaC (CloudFormation/Terraform).

## Buenas Prácticas

- **Claves primarias simples:** Usar claves simples y predecibles (`id` tipo UUID).
- **Índices secundarios:** Definir GSI/LSI solo si es necesario para queries específicas.
- **Evitar scans:** Usar queries por clave primaria o índices para eficiencia.
- **Tamaño de ítem:** Mantener los ítems pequeños (< 400 KB).
- **Backups:** Configurar backups automáticos en producción.
- **Provisionamiento:** Usar modo on-demand salvo que se requiera throughput fijo.

## Recomendaciones de Seguridad

- **Principio de menor privilegio:** El rol Lambda debe tener permisos mínimos sobre las tablas necesarias.
- **No exponer datos sensibles:** Evitar almacenar información sensible sin cifrado.
- **Auditoría:** Habilitar CloudTrail para monitoreo de accesos.

## Recursos Útiles

- [Documentación DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/)
- [SDK AWS para Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Best Practices DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)

---

> **Actualizado:** Julio 2025

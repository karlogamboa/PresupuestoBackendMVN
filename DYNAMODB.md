# Uso de DynamoDB en PresupuestoBackendMVN

Este documento explica cómo se utiliza DynamoDB en el proyecto PresupuestoBackendMVN, incluyendo la estructura de tablas y ejemplos de uso.

## Estructura de Tablas

Por defecto, cada ambiente tiene su propio prefijo de tabla. Ejemplo de tablas principales:

- **Solicitudes de Presupuesto**: `fin-dynamodb-<env>-presupuesto-solicitudes`
- **Proveedores**: `fin-dynamodb-<env>-presupuesto-proveedores`
- **Departamentos**: `fin-dynamodb-<env>-presupuesto-departamentos`
- **Categorías de Gasto**: `fin-dynamodb-<env>-presupuesto-categorias-gasto`
- **SCIM Users**: `fin-dynamodb-<env>-presupuesto-scim-users`
- **SCIM Groups**: `fin-dynamodb-<env>-presupuesto-scim-groups`

> **Nota:** El prefijo de las tablas incluye el valor del stage (`dev`, `qa`, `prod`) definido en la propiedad `stage` de configuración.

### Ejemplo de Estructura de Tabla: Solicitudes de Presupuesto

- **Nombre de la tabla**: `fin-dynamodb-dev-presupuesto-solicitudes`
- **Clave primaria**:
  - **Partition key**: `id` (String)
  - **Sort key**: `solicitudId` (String)
- **Índices secundarios**:
  - `numeroEmpleado-index`: Para buscar por número de empleado
  - `estatusConfirmacion-index`: Para buscar por estatus de confirmación

### Tipos de Datos

- **String**: Cadenas de texto.
- **Number**: Números (enteros o decimales).
- **Boolean**: Valores verdadero/falso.
- **List**: Listas de valores.
- **Map**: Objetos con pares clave-valor.

> **Nota:** Consulta los modelos Java para la estructura exacta y actualizada de cada entidad.

## Configuración de Acceso a DynamoDB

Asegúrate de tener las credenciales de AWS configuradas en tu entorno. Puedes hacerlo a través de variables de entorno, archivo de credenciales de AWS, o roles de IAM si estás ejecutando en AWS.

### Ejemplo de Variables de Entorno

```bash
export AWS_ACCESS_KEY_ID=tu_access_key_id
export AWS_SECRET_ACCESS_KEY=tu_secret_access_key
export AWS_REGION=us-east-1
```

### Ejemplo de Uso en Código Java

```java
@Autowired
private DynamoDbEnhancedClient enhancedClient;

public List<SolicitudPresupuesto> findAllSolicitudes() {
    DynamoDbTable<SolicitudPresupuesto> table = enhancedClient.table("fin-dynamodb-dev-presupuesto-solicitudes", TableSchema.fromBean(SolicitudPresupuesto.class));
    return table.scan().items().stream().collect(Collectors.toList());
}
```

## Consideraciones

- Asegúrate de manejar correctamente las excepciones al interactuar con DynamoDB.
- Utiliza los índices secundarios para mejorar el rendimiento de las consultas.
- Revisa las políticas de acceso a los recursos de AWS para garantizar la seguridad de los datos.

## Más Información

Para más detalles sobre cómo usar DynamoDB, consulta la [documentación oficial de DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html).
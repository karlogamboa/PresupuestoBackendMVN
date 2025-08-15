# Uso de DynamoDB en PresupuestoBackendMVN

Este documento explica cómo se utiliza DynamoDB en el proyecto PresupuestoBackendMVN, incluyendo la estructura de tablas y ejemplos de uso.

## Estructura de Tablas

Por defecto, cada ambiente tiene su propio prefijo de tabla. Ejemplo de tablas principales:

- **Solicitudes de Presupuesto**: `fin-dynamodb-<env>-presupuesto-solicitudes`
- **Proveedores**: `fin-dynamodb-<env>-presupuesto-proveedores`
- **Presupuestos**: `fin-dynamodb-<env>-presupuesto-presupuesto`
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

## Ejemplo AWS CLI para crear la tabla de presupuestos en CloudShell

```bash
aws dynamodb create-table \
  --table-name fin-dynamodb-qa-presupuesto-presupuestos \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-2
```

> Cambia el nombre de la tabla y la región según tu ambiente (`dev`, `qa`, `prod`).

## Consideraciones

- Asegúrate de manejar correctamente las excepciones al interactuar con DynamoDB.
- Utiliza los índices secundarios para mejorar el rendimiento de las consultas.
- Revisa las políticas de acceso a los recursos de AWS para garantizar la seguridad de los datos.

## Más Información

Para más detalles sobre cómo usar DynamoDB, consulta la [documentación oficial de DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html).




{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Effect": "Allow",
			"Action": [
				"dynamodb:Scan",
				"dynamodb:GetItem",
				"dynamodb:PutItem",
				"dynamodb:UpdateItem",
				"dynamodb:DeleteItem",
				"dynamodb:Query",
				"dynamodb:BatchWriteItem"
			],
			"Resource": [
				"arn:aws:dynamodb:us-east-2:470514032204:table/fin-dynamodb-qa-presupuesto-*",
				"arn:aws:dynamodb:us-east-2:470514032204:table/fin-dynamodb-qa-presupuesto-scim-*"
			]
		},
		{
			"Effect": "Allow",
			"Action": [
				"ses:SendEmail",
				"ses:SendRawEmail"
			],
			"Resource": "*"
		},
		{
			"Effect": "Allow",
			"Action": [
				"ssm:GetParameter",
				"ssm:GetParameters",
				"ssm:GetParametersByPath"
			],
			"Resource": "arn:aws:ssm:us-east-2:470514032204:parameter/fin/qa/*"
		},
		{
			"Effect": "Allow",
			"Action": [
				"logs:CreateLogGroup",
				"logs:CreateLogStream",
				"logs:PutLogEvents"
			],
			"Resource": "arn:aws:logs:us-east-2:*:*"
		}
	]
}
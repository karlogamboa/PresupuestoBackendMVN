#!/bin/bash

# Script para crear la tabla Proveedores en AWS DynamoDB
# Asegúrate de tener configuradas las credenciales de AWS (aws configure)
# Los datos se insertarán posteriormente desde un archivo CSV

echo "Creando tabla presupuesto-proveedores en AWS DynamoDB..."

aws dynamodb create-table \
    --table-name presupuesto-proveedores \
    --attribute-definitions \
        AttributeName=id,AttributeType=S \
    --key-schema \
        AttributeName=id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region us-east-2

if [ $? -eq 0 ]; then
    echo "✅ Tabla presupuesto-proveedores creada exitosamente"
    echo "Esperando que la tabla esté activa..."
    
    aws dynamodb wait table-exists \
        --table-name presupuesto-proveedores \
        --region us-east-2

    echo "✅ Tabla está activa y lista para usar"
else
    echo "❌ Error al crear la tabla"
    exit 1
fi

echo ""
echo "Verificando la tabla creada..."
aws dynamodb describe-table \
    --table-name presupuesto-proveedores \
    --region us-east-1 \
    --query 'Table.{TableName:TableName,Status:TableStatus,KeySchema:KeySchema[0].AttributeName,ItemCount:ItemCount,CreationDate:CreationDateTime}' \
    --output table

echo ""
echo "🎉 Tabla de proveedores creada exitosamente!"
echo ""
echo "📋 Estructura de la tabla:"
echo "  - Nombre: presupuesto-proveedores"
echo "  - Clave de partición: id (String)"
echo "  - Campos disponibles:"
echo "    • id - Identificador único del proveedor"
echo "    • Nombre - Nombre del proveedor"
echo "    • Categoría - Categoría del proveedor"
echo "    • Subsidiaria principal - Subsidiaria principal"
echo "    • Número Proveedor - Número de proveedor"
echo "    • Cuentas de gastos - Cuentas de gastos asociadas"
echo ""
echo "📄 Los datos se insertarán posteriormente usando el archivo CSV."
echo "   Puedes verificar la tabla con:"
echo "   aws dynamodb scan --table-name presupuesto-proveedores --region us-east-1"

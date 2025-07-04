#!/bin/bash

# Script para crear la tabla Departamentos en AWS DynamoDB
# Asegúrate de tener configuradas las credenciales de AWS (aws configure)

echo "Creando tabla presupuesto-departamentos en AWS DynamoDB..."

aws dynamodb create-table \
    --table-name presupuesto-departamentos \
    --attribute-definitions \
        AttributeName=id,AttributeType=S \
    --key-schema \
        AttributeName=id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region us-east-2

if [ $? -eq 0 ]; then
    echo "✅ Tabla presupuesto-departamentos creada exitosamente"
    echo "Esperando que la tabla esté activa..."
    
    aws dynamodb wait table-exists \
        --table-name presupuesto-departamentos \
        --region us-east-2

    echo "✅ Tabla está activa y lista para usar"
else
    echo "❌ Error al crear la tabla"
    exit 1
fi


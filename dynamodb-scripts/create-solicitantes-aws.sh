#!/bin/bash

# Script para crear la tabla Solicitantes en AWS DynamoDB
# Asegúrate de tener configuradas las credenciales de AWS (aws configure)

echo "Creando tabla presupuesto-solicitantes en AWS DynamoDB..."

aws dynamodb create-table \
    --table-name presupuesto-solicitantes \
    --attribute-definitions \
        AttributeName=numEmpleado,AttributeType=S \
    --key-schema \
        AttributeName=numEmpleado,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region us-east-2

if [ $? -eq 0 ]; then
    echo "✅ Tabla presupuesto-solicitantes creada exitosamente"
    echo "Esperando que la tabla esté activa..."
    
    aws dynamodb wait table-exists \
        --table-name presupuesto-solicitantes \
        --region us-east-2
    
    echo "✅ Tabla está activa y lista para usar"
else
    echo "❌ Error al crear la tabla"
    exit 1
fi

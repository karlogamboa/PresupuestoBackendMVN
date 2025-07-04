#!/bin/bash

# Script para crear la tabla Categorias de Gasto en AWS DynamoDB
# Asegúrate de tener configuradas las credenciales de AWS (aws configure)

echo "Creando tabla presupuesto-categorias-gasto en AWS DynamoDB..."

aws dynamodb create-table \
    --table-name presupuesto-categorias-gasto \
    --attribute-definitions \
        AttributeName=id,AttributeType=S \
    --key-schema \
        AttributeName=id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region us-east-1

if [ $? -eq 0 ]; then
    echo "✅ Tabla presupuesto-categorias-gasto creada exitosamente"
    echo "Esperando que la tabla esté activa..."
    
    aws dynamodb wait table-exists \
        --table-name presupuesto-categorias-gasto \
        --region us-east-1
    
    echo "✅ Tabla está activa y lista para usar"
else
    echo "❌ Error al crear la tabla"
    exit 1
fi

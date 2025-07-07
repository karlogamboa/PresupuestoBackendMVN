#!/bin/bash

# Script para crear la tabla de usuarios en DynamoDB
# Tabla: presupuesto-usuarios
# Partition Key: id (String)
# GSI: email-index (email como partition key)

TABLE_NAME="presupuesto-usuarios"
REGION="us-east-2"

echo "Creando tabla DynamoDB: $TABLE_NAME"

# Crear la tabla con id como partition key y GSI para email
aws dynamodb create-table \
    --table-name $TABLE_NAME \
    --attribute-definitions \
        AttributeName=id,AttributeType=S \
        AttributeName=email,AttributeType=S \
    --key-schema \
        AttributeName=id,KeyType=HASH \
    --global-secondary-indexes \
        '[
            {
                "IndexName": "email-index",
                "KeySchema": [
                    {
                        "AttributeName": "email",
                        "KeyType": "HASH"
                    }
                ],
                "Projection": {
                    "ProjectionType": "ALL"
                },
                "ProvisionedThroughput": {
                    "ReadCapacityUnits": 5,
                    "WriteCapacityUnits": 5
                }
            }
        ]' \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region $REGION

if [ $? -eq 0 ]; then
    echo "Tabla $TABLE_NAME creada exitosamente"
    
    # Esperar a que la tabla esté activa
    echo "Esperando a que la tabla esté activa..."
    aws dynamodb wait table-exists --table-name $TABLE_NAME --region $REGION
    
    if [ $? -eq 0 ]; then
        echo "Tabla $TABLE_NAME está activa y lista para usar"
        
        # Insertar usuario de prueba con rol Admin
        echo "Insertando usuario Admin de prueba..."
        aws dynamodb put-item \
            --table-name $TABLE_NAME \
            --item '{
                "id": {"S": "6854ce39dbbb061ad5f8c9ec"},
                "email": {"S": "karlo@zicral.com"},
                "nombre": {"S": "Karlo Zicral"},
                "numeroEmpleado": {"S": "12345"},
                "role": {"S": "Admin"},
                "departamento": {"S": "IT"},
                "area": {"S": "Desarrollo"},
                "activo": {"S": "true"},
                "fechaCreacion": {"S": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"},
                "fechaActualizacion": {"S": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"}
            }' \
            --region $REGION
        
        if [ $? -eq 0 ]; then
            echo "Usuario Admin insertado exitosamente"
        else
            echo "Error al insertar usuario Admin"
        fi
        
        # Insertar usuario de prueba con rol User
        echo "Insertando usuario User de prueba..."
        aws dynamodb put-item \
            --table-name $TABLE_NAME \
            --item '{
                "id": {"S": "6854ce39dbbb061ad5f8c9ed"},
                "email": {"S": "user@test.com"},
                "nombre": {"S": "Usuario Test"},
                "numeroEmpleado": {"S": "54321"},
                "role": {"S": "User"},
                "departamento": {"S": "Ventas"},
                "area": {"S": "Comercial"},
                "activo": {"S": "true"},
                "fechaCreacion": {"S": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"},
                "fechaActualizacion": {"S": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'"}
            }' \
            --region $REGION
        
        if [ $? -eq 0 ]; then
            echo "Usuario User insertado exitosamente"
        else
            echo "Error al insertar usuario User"
        fi
    else
        echo "Error: Tiempo de espera agotado esperando que la tabla esté activa"
        exit 1
    fi
else
    echo "Error al crear la tabla $TABLE_NAME"
    exit 1
fi

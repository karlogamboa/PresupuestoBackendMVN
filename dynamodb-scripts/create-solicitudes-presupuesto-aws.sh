#!/bin/bash

# DynamoDB Table Creation Script for SolicitudPresupuesto
# This script creates the DynamoDB table for budget requests (solicitudes de presupuesto) on AWS
# Note: This script is for AWS DynamoDB only, not for local DynamoDB

# Configuration
REGION="us-east-2"
TABLE_NAME="presupuesto-solicitudes"

echo "Creating DynamoDB table: $TABLE_NAME in region: $REGION"

# Create the table with composite key (id as partition key, solicitudId as sort key)
aws dynamodb create-table     --region $REGION     --table-name $TABLE_NAME     --attribute-definitions         AttributeName=id,AttributeType=S         AttributeName=solicitudId,AttributeType=S         AttributeName=numeroEmpleado,AttributeType=S         AttributeName=estatusConfirmacion,AttributeType=S     --key-schema         AttributeName=id,KeyType=HASH         AttributeName=solicitudId,KeyType=RANGE     --global-secondary-indexes         '[
            {
                "IndexName": "numeroEmpleado-index",
                "KeySchema": [
                    {
                        "AttributeName": "numeroEmpleado",
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
            },
            {
                "IndexName": "estatusConfirmacion-index",
                "KeySchema": [
                    {
                        "AttributeName": "estatusConfirmacion",
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
        ]'     --provisioned-throughput         ReadCapacityUnits=5,WriteCapacityUnits=5

# Wait for table to be created
echo "Waiting for table to be active..."
aws dynamodb wait table-exists --region $REGION --table-name $TABLE_NAME

# Check table status
echo "Table creation completed. Checking table status..."
aws dynamodb describe-table --region $REGION --table-name $TABLE_NAME --query 'Table.TableStatus'

echo "Table $TABLE_NAME created successfully!"
echo ""
echo "Table Details:"
echo "- Table Name: $TABLE_NAME"
echo "- Region: $REGION"
echo "- Partition Key: id (String)"
echo "- Sort Key: solicitudId (String)"
echo "- Global Secondary Indexes:"
echo "  - numeroEmpleado-index: For filtering by employee number"
echo "  - estatusConfirmacion-index: For filtering by status"
echo "- Capacity: 5 read/write units (can be adjusted as needed)"
echo ""
echo "Note: This table stores budget request data (solicitudes de presupuesto) with the following structure:"
echo "- id: Unique identifier for the budget request"
echo "- solicitudId: Request identifier (sort key)"
echo "- numeroEmpleado: Employee number (GSI)"
echo "- nombreSolicitante: Name of the requester"
echo "- areaId: Area identifier"
echo "- departamentoId: Department identifier"
echo "- subdepartamentoId: Sub-department identifier"
echo "- proveedorId: Supplier identifier"
echo "- categoriaGastoId: Expense category identifier"
echo "- descripcion: Description of the request"
echo "- monto: Amount requested"
echo "- moneda: Currency"
echo "- justificacion: Justification for the request"
echo "- estatusConfirmacion: Status (PENDIENTE, APROBADO, RECHAZADO) - GSI"
echo "- fechaCreacion: Creation timestamp"
echo "- fechaActualizacion: Last update timestamp"
echo "- creadoPor: Created by user"
echo "- actualizadoPor: Last updated by user"
echo "- archivosAdjuntos: List of attached files"
echo "- comentarios: Additional comments"

#!/bin/bash

echo "Consultando datos de la tabla Solicitantes (AWS DynamoDB)..."

# Listar todos los solicitantes
echo "=== TODOS LOS SOLICITANTES ==="
aws dynamodb scan \
    --table-name presupuesto-solicitantes \
    --region us-east-1 \
    --query 'Items[*].{NumEmpleado:numEmpleado.S,Nombre:Nombre.S,Departamento:Departamento.S,Subsidiaria:Subsidiaria.S,PuestoTrabajo:"Puesto de trabajo".S,AprobadorGastos:aprobadorGastos.BOOL}' \
    --output table

echo ""
echo "=== SOLICITANTES QUE SON APROBADORES ==="
aws dynamodb scan \
    --table-name presupuesto-solicitantes \
    --filter-expression "aprobadorGastos = :val" \
    --expression-attribute-values '{":val":{"BOOL":true}}' \
    --region us-east-1 \
    --query 'Items[*].{NumEmpleado:numEmpleado.S,Nombre:Nombre.S,Departamento:Departamento.S,PuestoTrabajo:"Puesto de trabajo".S}' \
    --output table

echo ""
echo "=== CONSULTAR UN SOLICITANTE ESPECÍFICO ==="
echo "Ingresa el número de empleado (ejemplo: EMP001):"
read NUM_EMPLEADO

if [ ! -z "$NUM_EMPLEADO" ]; then
    aws dynamodb get-item \
        --table-name presupuesto-solicitantes \
        --key "{\"numEmpleado\":{\"S\":\"$NUM_EMPLEADO\"}}" \
        --region us-east-1 \
        --query 'Item.{NumEmpleado:numEmpleado.S,Nombre:Nombre.S,Subsidiaria:Subsidiaria.S,Departamento:Departamento.S,PuestoTrabajo:"Puesto de trabajo".S,AprobadorGastos:aprobadorGastos.BOOL,IDInterno:"ID interno".N}' \
        --output table
fi

echo ""
echo "=== ESTADÍSTICAS DE LA TABLA ==="
aws dynamodb describe-table \
    --table-name presupuesto-solicitantes \
    --region us-east-1 \
    --query 'Table.{TableName:TableName,Status:TableStatus,ItemCount:ItemCount,CreationDate:CreationDateTime}' \
    --output table

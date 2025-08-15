package com.cdc.fin.presupuesto.repository.impl;

import com.cdc.fin.presupuesto.model.SolicitudPresupuesto;
import com.cdc.fin.presupuesto.repository.SolicitudPresupuestoRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
public class SolicitudPresupuestoRepositoryImpl implements SolicitudPresupuestoRepositoryCustom {

    @Autowired
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private static final String TABLE_NAME = "fin-dynamodb-qa-presupuesto-solicitudpresupuesto";

    // Cambia el método a public List<SolicitudPresupuesto> findByDynamicFilters(...)
    public List<SolicitudPresupuesto> findByDynamicFilters(Map<String, String> filters) {
        // Puedes inyectar un servicio aquí para obtener los datos reales de DynamoDB
        List<SolicitudPresupuesto> allSolicitudes = obtenerTodasLasSolicitudesDesdeDynamoDB();

        return allSolicitudes.stream()
            .filter(solicitud -> {
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value == null || value.isEmpty()) continue;
                    // Aplica filtro por campo (ajusta según tus atributos)
                    if ("estatus".equals(key) && !value.equalsIgnoreCase(solicitud.getEstatusConfirmacion())) return false;
                    if ("numeroEmpleado".equals(key) && !value.equalsIgnoreCase(solicitud.getNumeroEmpleado())) return false;
                    // Agrega más filtros según tus necesidades
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private List<SolicitudPresupuesto> obtenerTodasLasSolicitudesDesdeDynamoDB() {
        DynamoDbTable<SolicitudPresupuesto> table = dynamoDbEnhancedClient.table(
            TABLE_NAME, TableSchema.fromBean(SolicitudPresupuesto.class));
        return StreamSupport
            .stream(table.scan().items().spliterator(), false)
            .collect(Collectors.toList());
    }

}
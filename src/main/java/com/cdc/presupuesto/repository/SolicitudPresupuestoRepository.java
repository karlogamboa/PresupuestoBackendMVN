package com.cdc.presupuesto.repository;

import com.cdc.presupuesto.model.SolicitudPresupuesto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SolicitudPresupuestoRepository {

    private final DynamoDbTable<SolicitudPresupuesto> table;

    @Autowired
    public SolicitudPresupuestoRepository(DynamoDbEnhancedClient enhancedClient,
                                         @Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.table = enhancedClient.table(tablePrefix + "solicitudes", 
                                         TableSchema.fromBean(SolicitudPresupuesto.class));
    }

    public SolicitudPresupuesto save(SolicitudPresupuesto solicitud) {
        table.putItem(solicitud);
        return solicitud;
    }

    public Optional<SolicitudPresupuesto> findById(String id, String solicitudId) {
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(solicitudId)
                .build();
        
        SolicitudPresupuesto item = table.getItem(key);
        return Optional.ofNullable(item);
    }

    public List<SolicitudPresupuesto> findAll() {
        PageIterable<SolicitudPresupuesto> pages = table.scan(ScanEnhancedRequest.builder().build());
        return pages.items().stream().collect(Collectors.toList());
    }

    public List<SolicitudPresupuesto> findByNumEmpleado(String numEmpleado) {
        // Use GSI for numeroEmpleado
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(numEmpleado).build());
        
        return table.index("numeroEmpleado-index")
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(queryConditional)
                        .build())
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public void deleteById(String id, String solicitudId) {
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(solicitudId)
                .build();
        
        table.deleteItem(key);
    }

    public void deleteAll() {
        List<SolicitudPresupuesto> allSolicitudes = findAll();
        for (SolicitudPresupuesto solicitud : allSolicitudes) {
            deleteById(solicitud.getId(), solicitud.getSolicitudId());
        }
    }

    public void saveAll(List<SolicitudPresupuesto> solicitudes) {
        for (SolicitudPresupuesto solicitud : solicitudes) {
            save(solicitud);
        }
    }
}

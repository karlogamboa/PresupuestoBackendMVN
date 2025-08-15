package com.cdc.fin.presupuesto.repository;

import com.cdc.fin.presupuesto.model.SolicitudPresupuesto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SolicitudPresupuestoRepository {

    private final DynamoDbTable<SolicitudPresupuesto> table;

    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

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

    // Método paginado para buscar por número de empleado
    public Page<SolicitudPresupuesto> findByNumEmpleado(String numeroEmpleado, Pageable pageable) {
        List<SolicitudPresupuesto> all = findByNumEmpleado(numeroEmpleado);
        int total = all.size();
        int fromIndex = Math.min((int) pageable.getOffset(), total);
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<SolicitudPresupuesto> paged = all.subList(fromIndex, toIndex);
        return new PageImpl<>(paged, pageable, total);
    }

    // Método paginado para buscar todos
    public Page<SolicitudPresupuesto> findAll(Pageable pageable) {
        List<SolicitudPresupuesto> all = findAll();
        int total = all.size();
        int fromIndex = Math.min((int) pageable.getOffset(), total);
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<SolicitudPresupuesto> paged = all.subList(fromIndex, toIndex);
        return new PageImpl<>(paged, pageable, total);
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
        final int BATCH_SIZE = 25;
        for (int i = 0; i < solicitudes.size(); i += BATCH_SIZE) {
            List<SolicitudPresupuesto> batch = solicitudes.subList(i, Math.min(i + BATCH_SIZE, solicitudes.size()));
            WriteBatch.Builder<SolicitudPresupuesto> writeBatchBuilder = WriteBatch.builder(SolicitudPresupuesto.class).mappedTableResource(table);
            batch.forEach(writeBatchBuilder::addPutItem);
            enhancedClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                .writeBatches(writeBatchBuilder.build())
                .build());
        }
    }

    public Page<SolicitudPresupuesto> findByDynamicFilters(Map<String, String> filters, Pageable pageable) {
        // Escaneo completo y filtrado en memoria (útil para pocos registros)
        List<SolicitudPresupuesto> all = findAll().stream()
            .filter(s -> {
                boolean matches = true;
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value == null || value.isEmpty()) continue;
                    // Comparación dinámica por campo (búsqueda parcial, case-insensitive)
                    switch (key) {
                        case "numeroEmpleado":
                            matches &= s.getNumeroEmpleado() != null && s.getNumeroEmpleado().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "estatusConfirmacion":
                            matches &= s.getEstatusConfirmacion() != null && s.getEstatusConfirmacion().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "departamento":
                            matches &= s.getDepartamento() != null && s.getDepartamento().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "proveedor":
                            matches &= s.getProveedor() != null && s.getProveedor().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "solicitante":
                            matches &= s.getSolicitante() != null && s.getSolicitante().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "subDepartamento":
                            matches &= s.getSubDepartamento() != null && s.getSubDepartamento().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "categoriaGasto":
                            matches &= s.getCategoriaGasto() != null && s.getCategoriaGasto().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "cuentaGastos":
                            matches &= s.getCuentaGastos() != null && s.getCuentaGastos().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "periodoPresupuesto":
                            matches &= s.getPeriodoPresupuesto() != null && s.getPeriodoPresupuesto().toLowerCase().contains(value.toLowerCase());
                            break;
                        case "fecha":
                            matches &= s.getFecha() != null && s.getFecha().toLowerCase().contains(value.toLowerCase());
                            break;
                        // Agrega más campos según tu modelo
                        default:
                            // Si el campo no es reconocido, ignora el filtro
                            break;
                    }
                    if (!matches) break;
                }
                return matches;
            })
            .collect(Collectors.toList());

        int total = all.size();
        int fromIndex = Math.min((int) pageable.getOffset(), total);
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<SolicitudPresupuesto> paged = all.subList(fromIndex, toIndex);
        return new PageImpl<>(paged, pageable, total);
    }
}

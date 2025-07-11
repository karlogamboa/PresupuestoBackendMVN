package com.cdc.presupuesto.repository;

import com.cdc.presupuesto.model.Proveedor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProveedorRepository {

    private final DynamoDbTable<Proveedor> table;

    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

    @Autowired
    public ProveedorRepository(DynamoDbEnhancedClient enhancedClient,
                              @Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.table = enhancedClient.table(tablePrefix + "proveedores", 
                                         TableSchema.fromBean(Proveedor.class));
    }

    public Proveedor save(Proveedor proveedor) {
        table.putItem(proveedor);
        return proveedor;
    }

    public Optional<Proveedor> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        Proveedor item = table.getItem(key);
        return Optional.ofNullable(item);
    }

    public List<Proveedor> findAll() {
        PageIterable<Proveedor> pages = table.scan(ScanEnhancedRequest.builder().build());
        return pages.items().stream().collect(Collectors.toList());
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        table.deleteItem(key);
    }

    public void deleteAll() {
        List<Proveedor> allProveedores = findAll();
        for (Proveedor proveedor : allProveedores) {
            deleteById(proveedor.getId());
        }
    }

    public void saveAll(List<Proveedor> proveedores) {
        final int BATCH_SIZE = 25;
        for (int i = 0; i < proveedores.size(); i += BATCH_SIZE) {
            List<Proveedor> batch = proveedores.subList(i, Math.min(i + BATCH_SIZE, proveedores.size()));
            WriteBatch.Builder<Proveedor> writeBatchBuilder = WriteBatch.builder(Proveedor.class).mappedTableResource(table);
            batch.forEach(writeBatchBuilder::addPutItem);
            enhancedClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                .writeBatches(writeBatchBuilder.build())
                .build());
        }
    }
}

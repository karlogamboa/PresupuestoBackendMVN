package com.cdc.fin.presupuesto.repository;

import com.cdc.fin.presupuesto.model.Departamento;
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
public class DepartamentoRepository {

    private final DynamoDbTable<Departamento> table;

    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

    @Autowired
    public DepartamentoRepository(DynamoDbEnhancedClient enhancedClient,
                                 @Value("${aws.dynamodb.table.departamentos}") String tableName) {
        this.table = enhancedClient.table(tableName, 
                                         TableSchema.fromBean(Departamento.class));
    }

    public Departamento save(Departamento departamento) {
        table.putItem(departamento);
        return departamento;
    }

    public Optional<Departamento> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        Departamento item = table.getItem(key);
        return Optional.ofNullable(item);
    }

    public List<Departamento> findAll() {
        PageIterable<Departamento> pages = table.scan(ScanEnhancedRequest.builder().build());
        return pages.items().stream().collect(Collectors.toList());
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        table.deleteItem(key);
    }

    public void deleteAll() {
        List<Departamento> allDepartamentos = findAll();
        for (Departamento departamento : allDepartamentos) {
            deleteById(departamento.getId());
        }
    }

    public void saveAll(List<Departamento> departamentos) {
        final int BATCH_SIZE = 25;
        for (int i = 0; i < departamentos.size(); i += BATCH_SIZE) {
            List<Departamento> batch = departamentos.subList(i, Math.min(i + BATCH_SIZE, departamentos.size()));
            WriteBatch.Builder<Departamento> writeBatchBuilder = WriteBatch.builder(Departamento.class).mappedTableResource(table);
            batch.forEach(writeBatchBuilder::addPutItem);
            enhancedClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                .writeBatches(writeBatchBuilder.build())
                .build());
        }
    }

    // Busca el primer departamento que tenga el ceco dado (no es búsqueda por partition key)
    public Departamento findByCeco(String ceco) {
        return findAll().stream()
            .filter(d -> ceco != null && ceco.equals(d.getCeco()))
            .findFirst()
            .orElse(null);
    }
}
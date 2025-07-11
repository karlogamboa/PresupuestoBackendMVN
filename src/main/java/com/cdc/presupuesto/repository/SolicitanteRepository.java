package com.cdc.presupuesto.repository;

import com.cdc.presupuesto.model.Solicitante;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SolicitanteRepository {

    private final DynamoDbTable<Solicitante> table;

    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

    @Autowired
    public SolicitanteRepository(DynamoDbEnhancedClient enhancedClient,
                                @Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.table = enhancedClient.table(tablePrefix + "solicitantes", 
                                         TableSchema.fromBean(Solicitante.class));
    }

    public Solicitante save(Solicitante solicitante) {
        table.putItem(solicitante);
        return solicitante;
    }

    public Optional<Solicitante> findByNumEmpleado(String numEmpleado) {
        Key key = Key.builder().partitionValue(numEmpleado).build();
        Solicitante item = table.getItem(key);
        return Optional.ofNullable(item);
    }

    public List<Solicitante> findAll() {
        PageIterable<Solicitante> pages = table.scan(ScanEnhancedRequest.builder().build());
        return pages.items().stream().collect(Collectors.toList());
    }

    public void deleteByNumEmpleado(String numEmpleado) {
        Key key = Key.builder().partitionValue(numEmpleado).build();
        table.deleteItem(key);
    }

    public void deleteAll() {
        List<Solicitante> allSolicitantes = findAll();
        for (Solicitante solicitante : allSolicitantes) {
            deleteByNumEmpleado(solicitante.getNumEmpleado());
        }
    }

    public void saveAll(List<Solicitante> solicitantes) {
        saveAllBatch(solicitantes);
    }

    public void saveAllBatch(List<Solicitante> solicitantes) {
        final int BATCH_SIZE = 25;
        for (int i = 0; i < solicitantes.size(); i += BATCH_SIZE) {
            List<Solicitante> batch = solicitantes.subList(i, Math.min(i + BATCH_SIZE, solicitantes.size()));
            WriteBatch.Builder<Solicitante> writeBatchBuilder = WriteBatch.builder(Solicitante.class).mappedTableResource(table);
            batch.forEach(writeBatchBuilder::addPutItem);
            enhancedClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                .writeBatches(writeBatchBuilder.build())
                .build());
        }
    }
}

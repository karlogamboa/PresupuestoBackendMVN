package com.cdc.presupuesto.repository;

import com.cdc.presupuesto.model.Subdepartamento;
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
public class SubdepartamentoRepository {

    private final DynamoDbTable<Subdepartamento> table;

    @Autowired
    public SubdepartamentoRepository(DynamoDbEnhancedClient enhancedClient,
                                    @Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.table = enhancedClient.table(tablePrefix + "subdepartamentos", 
                                         TableSchema.fromBean(Subdepartamento.class));
    }

    public Subdepartamento save(Subdepartamento subdepartamento) {
        table.putItem(subdepartamento);
        return subdepartamento;
    }

    public Optional<Subdepartamento> findById(String areaId, String id) {
        Key key = Key.builder()
                .partitionValue(areaId)
                .sortValue(id)
                .build();
        
        Subdepartamento item = table.getItem(key);
        return Optional.ofNullable(item);
    }

    public List<Subdepartamento> findByAreaId(String areaId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(areaId).build());
        
        PageIterable<Subdepartamento> pages = table.query(QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build());
                
        return pages.items().stream().collect(Collectors.toList());
    }

    public List<Subdepartamento> findAll() {
        PageIterable<Subdepartamento> pages = table.scan(ScanEnhancedRequest.builder().build());
        return pages.items().stream().collect(Collectors.toList());
    }

    public void deleteById(String areaId, String id) {
        Key key = Key.builder()
                .partitionValue(areaId)
                .sortValue(id)
                .build();
        
        table.deleteItem(key);
    }
}

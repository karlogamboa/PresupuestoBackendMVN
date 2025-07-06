package com.cdc.presupuesto.repository;

import com.cdc.presupuesto.model.Departamento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DepartamentoRepository {

    private final DynamoDbTable<Departamento> table;

    @Autowired
    public DepartamentoRepository(DynamoDbEnhancedClient enhancedClient,
                                 @Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.table = enhancedClient.table(tablePrefix + "departamentos", 
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
        for (Departamento departamento : departamentos) {
            save(departamento);
        }
    }
}

package com.cdc.presupuesto.repository;

import com.cdc.presupuesto.model.CategoriaGasto;
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
public class CategoriaGastoRepository {

    private final DynamoDbTable<CategoriaGasto> table;

    @Autowired
    public CategoriaGastoRepository(DynamoDbEnhancedClient enhancedClient,
                                   @Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.table = enhancedClient.table(tablePrefix + "categorias-gasto", 
                                         TableSchema.fromBean(CategoriaGasto.class));
    }

    public CategoriaGasto save(CategoriaGasto categoria) {
        table.putItem(categoria);
        return categoria;
    }

    public Optional<CategoriaGasto> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        CategoriaGasto item = table.getItem(key);
        return Optional.ofNullable(item);
    }

    public List<CategoriaGasto> findAll() {
        PageIterable<CategoriaGasto> pages = table.scan(ScanEnhancedRequest.builder().build());
        return pages.items().stream().collect(Collectors.toList());
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        table.deleteItem(key);
    }

    public void deleteAll() {
        List<CategoriaGasto> allCategorias = findAll();
        for (CategoriaGasto categoria : allCategorias) {
            deleteById(categoria.getId());
        }
    }

    public void saveAll(List<CategoriaGasto> categorias) {
        for (CategoriaGasto categoria : categorias) {
            save(categoria);
        }
    }
}

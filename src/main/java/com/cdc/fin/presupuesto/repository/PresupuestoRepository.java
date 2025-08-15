package com.cdc.fin.presupuesto.repository;

import com.cdc.fin.presupuesto.model.Presupuesto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;

@Repository
public class PresupuestoRepository {

    private final DynamoDbTable<Presupuesto> table;

    @Autowired
    public PresupuestoRepository(DynamoDbEnhancedClient enhancedClient,
                                 @Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.table = enhancedClient.table(tablePrefix + "presupuestos",
                                         TableSchema.fromBean(Presupuesto.class));
    }

    public Presupuesto save(Presupuesto presupuesto) {
        table.putItem(presupuesto);
        return presupuesto;
    }

    public void saveAll(List<Presupuesto> presupuestos) {
        for (Presupuesto p : presupuestos) {
            save(p);
        }
    }

    public List<Presupuesto> findAll() {
        PageIterable<Presupuesto> pages = table.scan(ScanEnhancedRequest.builder().build());
        return pages.items().stream().toList();
    }


    public void deleteAll() {
        for (Presupuesto p : findAll()) {
            table.deleteItem(p);
        }
    }

    public Optional<Presupuesto> findByCecoAndCuentaGastos(String ceco, String cuentaGastos) {
        List<Presupuesto> all = findAll();
        return all.stream()
            .filter(p -> ceco.equals(p.getCeco()) &&
                         cuentaGastos.equals(p.getCuentaGastos()))
            .findFirst();
    }
}

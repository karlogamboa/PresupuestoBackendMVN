package com.cdc.fin.presupuesto.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
public class Presupuesto {

    private String id;
    private String ceco;
    private String cuentaGastos;
    private String presupuesto;
    private String fechaInicial;
    private String fechaFinal;

    public Presupuesto() {}

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAttribute("CeCo")
    public String getCeco() {
        return ceco;
    }

    public void setCeco(String ceco) {
        this.ceco = ceco;
    }

    @DynamoDbAttribute("CuentaGastos")
    public String getCuentaGastos() {
        return cuentaGastos;
    }

    public void setCuentaGastos(String cuentaGastos) {
        this.cuentaGastos = cuentaGastos;
    }

    @DynamoDbAttribute("Presupuesto")
    public String getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(String presupuesto) {
        this.presupuesto = presupuesto;
    }

    @DynamoDbAttribute("FechaInicial")
    public String getFechaInicial() {
        return fechaInicial;
    }

    public void setFechaInicial(String fechaInicial) {
        this.fechaInicial = fechaInicial;
    }

    @DynamoDbAttribute("FechaFinal")
    public String getFechaFinal() {
        return fechaFinal;
    }

    public void setFechaFinal(String fechaFinal) {
        this.fechaFinal = fechaFinal;
    }
}

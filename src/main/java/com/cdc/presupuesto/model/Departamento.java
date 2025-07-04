package com.cdc.presupuesto.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
public class Departamento {
    
    private String id;
    private String nombreDepartamento;
    private String subDepartamento;
    private String ceco;

    public Departamento() {}

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAttribute("Departamento")
    public String getNombreDepartamento() {
        return nombreDepartamento;
    }

    public void setNombreDepartamento(String nombreDepartamento) {
        this.nombreDepartamento = nombreDepartamento;
    }

    @DynamoDbAttribute("Sub-Departamento")
    public String getSubDepartamento() {
        return subDepartamento;
    }

    public void setSubDepartamento(String subDepartamento) {
        this.subDepartamento = subDepartamento;
    }

    @DynamoDbAttribute("CeCo")
    public String getCeco() {
        return ceco;
    }

    public void setCeco(String ceco) {
        this.ceco = ceco;
    }
}

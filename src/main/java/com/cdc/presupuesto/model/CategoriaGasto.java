package com.cdc.presupuesto.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
public class CategoriaGasto {
    
    private String id = "";
    private String nombre = "";
    private String descripcion = "";
    private String cuentaDeGastos = "";
    private String cuenta = "";
    private double saldo = 0.0;

    public CategoriaGasto() {}

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAttribute("Nombre")
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @DynamoDbAttribute("Descripción")
    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @DynamoDbAttribute("Cuenta de gastos")
    public String getCuentaDeGastos() {
        return cuentaDeGastos;
    }

    public void setCuentaDeGastos(String cuentaDeGastos) {
        this.cuentaDeGastos = cuentaDeGastos;
    }

    @DynamoDbAttribute("Cuenta")
    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    @DynamoDbAttribute("Saldo")
    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
}

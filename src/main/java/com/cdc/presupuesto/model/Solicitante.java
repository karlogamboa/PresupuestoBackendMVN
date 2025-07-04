package com.cdc.presupuesto.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
public class Solicitante {
    
    private String numEmpleado; // Partition Key
    private String nombre = "";
    private String subsidiaria = "";
    private String departamento = "";
    private String puestoTrabajo = "";
    private boolean aprobadorGastos = false;
    private int idInterno = 0;

    public Solicitante() {}

    @DynamoDbPartitionKey
    public String getNumEmpleado() {
        return numEmpleado;
    }

    public void setNumEmpleado(String numEmpleado) {
        this.numEmpleado = numEmpleado;
    }

    @DynamoDbAttribute("Nombre")
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @DynamoDbAttribute("Subsidiaria")
    public String getSubsidiaria() {
        return subsidiaria;
    }

    public void setSubsidiaria(String subsidiaria) {
        this.subsidiaria = subsidiaria;
    }

    @DynamoDbAttribute("Departamento")
    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    @DynamoDbAttribute("Puesto de trabajo")
    public String getPuestoTrabajo() {
        return puestoTrabajo;
    }

    public void setPuestoTrabajo(String puestoTrabajo) {
        this.puestoTrabajo = puestoTrabajo;
    }

    public boolean isAprobadorGastos() {
        return aprobadorGastos;
    }

    public void setAprobadorGastos(boolean aprobadorGastos) {
        this.aprobadorGastos = aprobadorGastos;
    }

    @DynamoDbAttribute("ID interno")
    public int getIdInterno() {
        return idInterno;
    }

    public void setIdInterno(int idInterno) {
        this.idInterno = idInterno;
    }

    @Override
    public String toString() {
        return "Solicitante{" +
                "numEmpleado='" + numEmpleado + '\'' +
                ", nombre='" + nombre + '\'' +
                ", subsidiaria='" + subsidiaria + '\'' +
                ", departamento='" + departamento + '\'' +
                ", puestoTrabajo='" + puestoTrabajo + '\'' +
                ", aprobadorGastos=" + aprobadorGastos +
                ", idInterno=" + idInterno +
                '}';
    }
}

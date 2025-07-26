package com.cdc.fin.presupuesto.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
public class SolicitudPresupuesto {
    
    private String id;
    private String solicitudId;
    private String solicitante;
    private String numeroEmpleado;
    private String correo;
    private String cecos;
    private String departamento;
    private String subDepartamento;
    private String centroCostos;
    
    private String categoriaGasto;
    
    private String cuentaGastos;
    
    private String nombre;
    
    private String presupuestoDepartamento;
    
    private String presupuestoArea;
    
    private double montoSubtotal;
    
    private String estatusConfirmacion;
    
    private String fecha;
    
    private String periodoPresupuesto;
    
    private String empresa;
    
    private String proveedor;
    
    private Instant fechaCreacion;
    
    private Instant fechaActualizacion;
    
    private String creadoPor;
    
    private String actualizadoPor;
    
    private List<String> archivosAdjuntos;
    
    private String comentarios;

    public SolicitudPresupuesto() {}

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSortKey
    public String getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(String solicitudId) {
        this.solicitudId = solicitudId;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"numeroEmpleado-index"})
    public String getNumeroEmpleado() {
        return numeroEmpleado;
    }

    public void setNumeroEmpleado(String numeroEmpleado) {
        this.numeroEmpleado = numeroEmpleado;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCecos() {
        return cecos;
    }

    public void setCecos(String cecos) {
        this.cecos = cecos;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getSubDepartamento() {
        return subDepartamento;
    }

    public void setSubDepartamento(String subDepartamento) {
        this.subDepartamento = subDepartamento;
    }

    public String getCentroCostos() {
        return centroCostos;
    }

    public void setCentroCostos(String centroCostos) {
        this.centroCostos = centroCostos;
    }

    public String getCategoriaGasto() {
        return categoriaGasto;
    }

    public void setCategoriaGasto(String categoriaGasto) {
        this.categoriaGasto = categoriaGasto;
    }

    public String getCuentaGastos() {
        return cuentaGastos;
    }

    public void setCuentaGastos(String cuentaGastos) {
        this.cuentaGastos = cuentaGastos;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPresupuestoDepartamento() {
        return presupuestoDepartamento;
    }

    public void setPresupuestoDepartamento(String presupuestoDepartamento) {
        this.presupuestoDepartamento = presupuestoDepartamento;
    }

    public String getPresupuestoArea() {
        return presupuestoArea;
    }

    public void setPresupuestoArea(String presupuestoArea) {
        this.presupuestoArea = presupuestoArea;
    }

    public double getMontoSubtotal() {
        return montoSubtotal;
    }

    public void setMontoSubtotal(double montoSubtotal) {
        this.montoSubtotal = montoSubtotal;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"estatusConfirmacion-index"})
    public String getEstatusConfirmacion() {
        return estatusConfirmacion;
    }

    public void setEstatusConfirmacion(String estatusConfirmacion) {
        this.estatusConfirmacion = estatusConfirmacion;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getPeriodoPresupuesto() {
        return periodoPresupuesto;
    }

    public void setPeriodoPresupuesto(String periodoPresupuesto) {
        this.periodoPresupuesto = periodoPresupuesto;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public String getActualizadoPor() {
        return actualizadoPor;
    }

    public void setActualizadoPor(String actualizadoPor) {
        this.actualizadoPor = actualizadoPor;
    }

    public List<String> getArchivosAdjuntos() {
        return archivosAdjuntos;
    }

    public void setArchivosAdjuntos(List<String> archivosAdjuntos) {
        this.archivosAdjuntos = archivosAdjuntos;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }
}
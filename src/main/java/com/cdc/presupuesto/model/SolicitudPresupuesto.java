package com.cdc.presupuesto.model;

import io.swagger.v3.oas.annotations.media.Schema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
@Schema(description = "Solicitud de Presupuesto - Representa una solicitud de presupuesto en el sistema CDC")
public class SolicitudPresupuesto {
    
    @Schema(description = "Identificador único de la solicitud", example = "uuid-12345")
    private String id;
    
    @Schema(description = "ID de la solicitud", example = "REQ-1234567890")
    private String solicitudId;
    
    @Schema(description = "Nombre del solicitante", example = "Juan Pérez")
    private String solicitante;
    
    @Schema(description = "Número de empleado", example = "EMP001")
    private String numeroEmpleado;
    
    @Schema(description = "Correo electrónico del solicitante", example = "juan.perez@cdc.com")
    private String correo;
    
    @Schema(description = "Centro de costos", example = "CC001")
    private String cecos;
    
    @Schema(description = "Departamento", example = "Tecnología")
    private String departamento;
    
    @Schema(description = "Subdepartamento", example = "Desarrollo")
    private String subDepartamento;
    
    @Schema(description = "Centro de costos", example = "TI-2024")
    private String centroCostos;
    
    @Schema(description = "Categoría de gasto", example = "Software")
    private String categoriaGasto;
    
    @Schema(description = "Cuenta de gastos", example = "GASTOS-SW")
    private String cuentaGastos;
    
    @Schema(description = "Nombre del producto/servicio", example = "Licencia Microsoft Office")
    private String nombre;
    
    @Schema(description = "Presupuesto del departamento", example = "50000.00")
    private String presupuestoDepartamento;
    
    @Schema(description = "Presupuesto del área", example = "200000.00")
    private String presupuestoArea;
    
    @Schema(description = "Monto subtotal de la solicitud", example = "1500.00")
    private double montoSubtotal;
    
    @Schema(description = "Estatus de confirmación", 
            example = "PENDIENTE", 
            allowableValues = {"PENDIENTE", "APROBADO", "RECHAZADO"})
    private String estatusConfirmacion;
    
    @Schema(description = "Fecha de la solicitud", example = "2024-01-15")
    private String fecha;
    
    @Schema(description = "Período presupuestario", example = "2024-Q1")
    private String periodoPresupuesto;
    
    @Schema(description = "Empresa", example = "CDC")
    private String empresa;
    
    @Schema(description = "Proveedor", example = "Microsoft Corporation")
    private String proveedor;
    
    @Schema(description = "Fecha de creación del registro")
    private Instant fechaCreacion;
    
    @Schema(description = "Fecha de última actualización")
    private Instant fechaActualizacion;
    
    @Schema(description = "Usuario que creó el registro")
    private String creadoPor;
    
    @Schema(description = "Usuario que actualizó por última vez")
    private String actualizadoPor;
    
    @Schema(description = "Lista de archivos adjuntos")
    private List<String> archivosAdjuntos;
    
    @Schema(description = "Comentarios adicionales")
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

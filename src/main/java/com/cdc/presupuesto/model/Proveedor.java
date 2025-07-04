package com.cdc.presupuesto.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
public class Proveedor {
    
    private String id = "";
    private String nombre = "";
    private String duplicado = "";
    private String categoria = "";
    private String subsidiariaPrincipal = "";
    private String contactoPrincipal = "";
    private String telefono = "";
    private String correoElectronico = "";
    private String accesoInicioSesion = "";
    private String numeroProveedor = "";
    private String cuentasGastos = "";

    public Proveedor() {}

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAttribute("nombre")
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @DynamoDbAttribute("duplicado")
    public String getDuplicado() {
        return duplicado;
    }

    public void setDuplicado(String duplicado) {
        this.duplicado = duplicado;
    }

    @DynamoDbAttribute("categoria")
    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    @DynamoDbAttribute("subsidiaria_principal")
    public String getSubsidiariaPrincipal() {
        return subsidiariaPrincipal;
    }

    public void setSubsidiariaPrincipal(String subsidiariaPrincipal) {
        this.subsidiariaPrincipal = subsidiariaPrincipal;
    }

    @DynamoDbAttribute("contacto_principal")
    public String getContactoPrincipal() {
        return contactoPrincipal;
    }

    public void setContactoPrincipal(String contactoPrincipal) {
        this.contactoPrincipal = contactoPrincipal;
    }

    @DynamoDbAttribute("telefono")
    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    @DynamoDbAttribute("correo_electronico")
    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    @DynamoDbAttribute("acceso_inicio_sesion")
    public String getAccesoInicioSesion() {
        return accesoInicioSesion;
    }

    public void setAccesoInicioSesion(String accesoInicioSesion) {
        this.accesoInicioSesion = accesoInicioSesion;
    }

    @DynamoDbAttribute("numero_proveedor")
    public String getNumeroProveedor() {
        return numeroProveedor;
    }

    public void setNumeroProveedor(String numeroProveedor) {
        this.numeroProveedor = numeroProveedor;
    }

    @DynamoDbAttribute("cuentas_gastos")
    public String getCuentasGastos() {
        return cuentasGastos;
    }

    public void setCuentasGastos(String cuentasGastos) {
        this.cuentasGastos = cuentasGastos;
    }

    @Override
    public String toString() {
        return "Proveedor{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", duplicado='" + duplicado + '\'' +
                ", categoria='" + categoria + '\'' +
                ", subsidiariaPrincipal='" + subsidiariaPrincipal + '\'' +
                ", contactoPrincipal='" + contactoPrincipal + '\'' +
                ", telefono='" + telefono + '\'' +
                ", correoElectronico='" + correoElectronico + '\'' +
                ", accesoInicioSesion='" + accesoInicioSesion + '\'' +
                ", numeroProveedor='" + numeroProveedor + '\'' +
                ", cuentasGastos='" + cuentasGastos + '\'' +
                '}';
    }
}

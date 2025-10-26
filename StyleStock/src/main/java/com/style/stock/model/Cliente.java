package com.style.stock.model;

public class Cliente {
    private int id;
    private String nombre;
    private String direccion;
    private String telefono;
    private String cuit;

    public Cliente(int id, String nombre, String direccion, String telefono, String cuit) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.cuit = cuit;
    }

    public Cliente(String nombre, String direccion, String telefono, String cuit) {
        this(-1, nombre, direccion, telefono, cuit);
    }

    // Getters and setters...
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public String getCuit() { return cuit; }

    public void setId(int id) { this.id = id; }

    @Override
    public String toString() {
        return nombre;
    }
}

package com.style.stock.model;

public class Producto {
    private int id;
    private String codigo;
    private String descripcion;
    private double precio;
    private int stock;

    public Producto(int id, String codigo, String descripcion, double precio, int stock) {
        this.id = id;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
    }

    public Producto(String codigo, String descripcion, double precio, int stock) {
        this(-1, codigo, descripcion, precio, stock);
    }

    // Getters and setters...
    public int getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public double getPrecio() { return precio; }
    public int getStock() { return stock; }

    public void setId(int id) { this.id = id; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public String toString() {
        return descripcion; // o return codigo + " - " + descripcion;
    }
}

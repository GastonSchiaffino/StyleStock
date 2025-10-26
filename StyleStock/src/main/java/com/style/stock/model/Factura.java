package com.style.stock.model;

public class Factura {
    private int id;
    private int clienteId;
    private String fecha;
    private double total;
    private String tipo;


    public Factura(int id, int clienteId, String fecha, double total, String tipo) {
        this.id = id;
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.total = total;
        this.tipo = tipo;
    }

    // Getters and setters...
    public int getId() { return id; }
    public int getClienteId() { return clienteId; }
    public String getFecha() { return fecha; }
    public double getTotal() { return total; }
    public String getTipo() { return tipo; }
}

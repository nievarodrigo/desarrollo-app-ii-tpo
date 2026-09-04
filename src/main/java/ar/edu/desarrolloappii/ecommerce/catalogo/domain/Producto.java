package ar.edu.desarrolloappii.ecommerce.catalogo.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Producto del catálogo.
 *
 * Hoy es un POJO que se hidrata desde el JSON semilla. Cuando incorporemos JPA
 * pasa a ser una {@code @Entity} y el JSON queda sólo como carga inicial: por
 * eso el resto del sistema nunca lo devuelve tal cual, sino a través de
 * {@code ProductoResponse} (patrón DTO).
 */
public class Producto {

    private long id;
    private String sku;
    private String nombre;
    private String marca;
    private String categoria;
    private BigDecimal precio;
    private BigDecimal precioAnterior;
    private int stock;
    private double rating;
    private int resenias;
    private boolean activo;
    private String arte;
    private String descripcion;
    private List<String> destacados = new ArrayList<>();
    private List<String> etiquetas = new ArrayList<>();

    /** Requerido por JSON-B para deserializar la semilla. */
    public Producto() {
    }

    /** Regla de negocio: se puede comprar sólo si está activo y hay unidades. */
    public boolean hayStock() {
        return activo && stock > 0;
    }

    public boolean estaDestacado() {
        return etiquetas.contains("destacado");
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public BigDecimal getPrecioAnterior() { return precioAnterior; }
    public void setPrecioAnterior(BigDecimal precioAnterior) { this.precioAnterior = precioAnterior; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getResenias() { return resenias; }
    public void setResenias(int resenias) { this.resenias = resenias; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getArte() { return arte; }
    public void setArte(String arte) { this.arte = arte; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<String> getDestacados() { return destacados; }
    public void setDestacados(List<String> destacados) {
        this.destacados = destacados == null ? new ArrayList<>() : destacados;
    }

    public List<String> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(List<String> etiquetas) {
        this.etiquetas = etiquetas == null ? new ArrayList<>() : etiquetas;
    }
}

package ar.edu.desarrolloappii.ecommerce.catalogo.domain;

/** Categoría del catálogo. Clasifica productos; no contiene reglas propias. */
public class Categoria {

    private long id;
    private String slug;
    private String nombre;
    private String bajada;
    private String icono;

    /** Requerido por JSON-B para deserializar la semilla. */
    public Categoria() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getBajada() { return bajada; }
    public void setBajada(String bajada) { this.bajada = bajada; }

    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
}

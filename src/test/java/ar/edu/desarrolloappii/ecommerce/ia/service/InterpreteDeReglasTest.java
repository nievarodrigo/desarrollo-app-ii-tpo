package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.CriteriosSugeridos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InterpreteDeReglasTest {
    private final InterpreteDeReglas reglas = new InterpreteDeReglas();

    @Test void entiendeCategoriaOrdenYPrecio() {
        CriteriosSugeridos c = reglas.interpretar("Quiero auriculares baratos hasta 50 mil").orElseThrow();
        assertEquals("audio", c.categoria());
        assertEquals("precio-asc", c.orden());
        assertEquals(50_000.0, c.precioMax());
        assertEquals("auricular", c.q());
    }

    @Test void entiendePreciosConPuntosYSimbolo() {
        assertEquals(120_000.0, InterpreteDeReglas.precio("hasta $120.000"));
        assertEquals(80_000.0, InterpreteDeReglas.precio("menos de 80k"));
        assertNull(InterpreteDeReglas.precio("sin precio"));
    }

    @Test void entiendeStockYMejorValorados() {
        CriteriosSugeridos c = reglas.interpretar("los mejor valorados de deportes en stock").orElseThrow();
        assertEquals("deportes", c.categoria());
        assertEquals("rating", c.orden());
        assertTrue(c.soloStock());
    }

    @Test void siNoEntiendeNadaNoInventa() {
        assertTrue(reglas.interpretar("hola").isEmpty());
    }
}

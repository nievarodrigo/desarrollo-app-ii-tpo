package ar.edu.desarrolloappii.ecommerce.seguridad.domain;

import java.time.Instant;

/** Cuenta guardada en el servidor. La clave sólo existe como hash con sal. */
public record Usuario(String nombre, String email, Rol rol, String claveHash, Instant creadoEn) {
    public Identidad identidad() { return new Identidad(email, rol); }
}

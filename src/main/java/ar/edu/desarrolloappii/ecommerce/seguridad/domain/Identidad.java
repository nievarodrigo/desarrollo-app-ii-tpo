package ar.edu.desarrolloappii.ecommerce.seguridad.domain;

/** Quién está haciendo el pedido HTTP: sale de un JWT ya verificado. */
public record Identidad(String email, Rol rol) { }

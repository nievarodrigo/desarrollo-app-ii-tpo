package ar.edu.desarrolloappii.ecommerce.seguridad.controller;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import ar.edu.desarrolloappii.ecommerce.seguridad.service.AutorizadorDeAcceso;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;

/**
 * Filtro que corre antes de cada método REST: si el método está protegido, valida el JWT y el rol.
 * Si falla, lanza 401/403 (los traduce ErrorDeNegocioMapper). Si pasa, deja la identidad disponible
 * en el SecurityContext para que el controller sepa quién llama.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AutenticacionFilter implements ContainerRequestFilter {
    @Context ResourceInfo recurso;
    @Inject AutorizadorDeAcceso autorizador;

    @Override
    public void filter(ContainerRequestContext peticion) {
        if (recurso == null || recurso.getResourceMethod() == null) return; // 404/405 los resuelve JAX-RS
        autorizador.autorizar(recurso.getResourceMethod(), peticion.getHeaderString(HttpHeaders.AUTHORIZATION))
                .ifPresent(identidad -> peticion.setSecurityContext(contexto(identidad, peticion.getSecurityContext())));
    }

    private static SecurityContext contexto(Identidad identidad, SecurityContext original) {
        return new SecurityContext() {
            @Override public Principal getUserPrincipal() { return identidad::email; }
            @Override public boolean isUserInRole(String rol) { return identidad.rol().name().equals(rol); }
            @Override public boolean isSecure() { return original != null && original.isSecure(); }
            @Override public String getAuthenticationScheme() { return "Bearer"; }
        };
    }

    /** Para los controllers: reconstruye la identidad desde el SecurityContext. */
    public static Identidad identidadDe(SecurityContext contexto) {
        String email = contexto.getUserPrincipal().getName();
        return new Identidad(email, contexto.isUserInRole("ADMIN") ? Rol.ADMIN : Rol.CLIENTE);
    }
}

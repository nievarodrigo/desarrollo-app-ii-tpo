package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.AccesoDenegadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.NoAutenticadoException;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * Decide si un pedido HTTP puede ejecutar un método del controller.
 *
 * Reglas (se leen de las anotaciones estándar de Jakarta):
 *  - sin anotación o @PermitAll → público;
 *  - @RolesAllowed({...}) → exige token válido (si no, 401) y que el rol esté en la lista (si no, 403);
 *  - @DenyAll → 403 siempre.
 * Es una clase común (sin JAX-RS) para poder probarla con tests unitarios.
 */
@ApplicationScoped
public class AutorizadorDeAcceso {
    private static final String PREFIJO = "Bearer ";
    private JwtService jwt;

    protected AutorizadorDeAcceso() { } // para el proxy de CDI

    @Inject
    public AutorizadorDeAcceso(JwtService jwt) { this.jwt = jwt; }

    /** @return la identidad si el endpoint es protegido y el acceso es válido; vacío si es público. */
    public Optional<Identidad> autorizar(Method metodo, String cabeceraAuthorization) {
        // La regla del método reemplaza la de clase, también para DenyAll y PermitAll.
        boolean reglaEnMetodo = metodo.isAnnotationPresent(DenyAll.class)
                || metodo.isAnnotationPresent(PermitAll.class) || metodo.isAnnotationPresent(RolesAllowed.class);
        java.lang.reflect.AnnotatedElement origen = reglaEnMetodo ? metodo : metodo.getDeclaringClass();
        if (origen.isAnnotationPresent(DenyAll.class)) throw new AccesoDenegadoException("Operación no disponible.");
        RolesAllowed roles = origen.getAnnotation(RolesAllowed.class);
        if (roles == null) return Optional.empty();

        if (cabeceraAuthorization == null
                || !cabeceraAuthorization.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            throw new NoAutenticadoException("Falta el encabezado Authorization: Bearer <token>.");
        }
        Identidad identidad = jwt.verificar(cabeceraAuthorization.substring(PREFIJO.length()).trim());
        if (Arrays.stream(roles.value()).noneMatch(r -> r.equals(identidad.rol().name()))) {
            throw new AccesoDenegadoException("Tu rol (" + identidad.rol() + ") no puede hacer esto.");
        }
        return Optional.of(identidad);
    }
}

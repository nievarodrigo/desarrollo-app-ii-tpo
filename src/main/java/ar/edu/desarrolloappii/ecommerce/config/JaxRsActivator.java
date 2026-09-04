package ar.edu.desarrolloappii.ecommerce.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activa JAX-RS y fija el prefijo de la API.
 *
 * Con esta clase no hace falta {@code web.xml}. El WAR se llama
 * {@code ecommerce.war}, así que los recursos quedan publicados bajo
 * {@code /ecommerce/api/...}.
 */
@ApplicationPath("/api")
public class JaxRsActivator extends Application {
}

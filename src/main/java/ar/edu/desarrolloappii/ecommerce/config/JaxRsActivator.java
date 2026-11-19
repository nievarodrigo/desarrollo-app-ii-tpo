package ar.edu.desarrolloappii.ecommerce.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activa JAX-RS y fija el prefijo de la API, con la VERSIÓN en la URI (Clase 10: la opción
 * recomendada para el TP). Un cambio incompatible del contrato se publicaría como /api/v2
 * sin romper a los consumidores de /api/v1.
 *
 * Con esta clase no hace falta {@code web.xml}. El WAR se llama
 * {@code ecommerce.war}, así que los recursos quedan publicados bajo
 * {@code /ecommerce/api/v1/...}.
 */
@ApplicationPath("/api/v1")
public class JaxRsActivator extends Application {
}

package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/** Prepara el esquema al arrancar para que los Repository encuentren sus tablas. */
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class Migraciones {
    @Resource(lookup = "java:jboss/datasources/GomarketDS")
    DataSource fuenteDeDatos;

    /** Aplica las migraciones pendientes para dejar listo el esquema que usa JPA. */
    @PostConstruct
    public void iniciar() {
        Flyway.configure()
                .dataSource(fuenteDeDatos)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
    }
}

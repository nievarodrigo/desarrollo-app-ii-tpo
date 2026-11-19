package ar.edu.desarrolloappii.ecommerce.pedido.repository.jpa;
import jakarta.annotation.*;
import jakarta.ejb.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
@Singleton @Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class Migraciones {
 @Resource(lookup="java:jboss/datasources/GomarketDS") DataSource datasource;
 @PostConstruct public void iniciar() { Flyway.configure().dataSource(datasource).baselineOnMigrate(true).baselineVersion("0").load().migrate(); }
}

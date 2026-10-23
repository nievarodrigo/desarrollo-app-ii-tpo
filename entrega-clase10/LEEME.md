# Entrega de Clase 10 — Servicios REST (09/10/2026)

La foto de la consigna pide **cinco entregables de diseño** y ofrece un
**bonus de implementación**. No pide escribir seis endpoints nuevos ni que
todo se entregue como código.

## Qué mostrar o subir

1. **`ENTREGA.pdf`**: catálogo de 7 endpoints reales (mínimo 6), decisiones
   de idempotencia/versionado/seguridad y caso timeout SOAP.
2. **`openapi.yaml`**: contrato OpenAPI de 2 endpoints reales de Pedido,
   incluido un error.
3. **`ejemplo-exito.json` y `ejemplo-error-problem.json`**: request/response
   exitoso y error Problem Details.
4. **`secuencia.pdf`**: diagrama REST propio → SOAP legado → evento → REST
   externo. Punta llena = síncrono, punta abierta = asíncrono; azul =
   implementado, naranja = sólo diseño. Un bloque `alt` muestra OK, Fault y
   timeout de 5 s del SOAP.
5. **Bonus:** `GOMARKET.postman_collection.json` se ejecutó en Postman
   Desktop contra WildFly: 202, 200 y 400 Problem Details, con todos los
   tests OK. Capturas en `capturas-postman/` y en la sección 7 del PDF.

El servidor corre con:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./mvnw clean package
../wildfly/wildfly-41-ee10/bin/standalone.sh -c standalone-full.xml
```

Desplegar `target/ecommerce.war` y probar
`http://localhost:8080/ecommerce/api/salud`. La cola JMS requiere
`standalone-full.xml`.

**No afirmar:** que SOAP, el tópico, el REST externo, pago real, reserva de
stock, seguridad del endpoint o base de datos estén implementados. Lo que sí
funciona es REST + cola JMS + consumidor MDB.

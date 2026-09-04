# Entrega — Checkpoint 1

**Proyecto:** TPO Store — e-commerce orientado a componentes  
**Materia:** Desarrollo de Aplicaciones II  
**Equipo:** Rodrigo, Nicolás, Tomás, Iván y Fol  
**Documento técnico:** [Checkpoint_1_Ecommerce.pdf](Checkpoint_1_Ecommerce.pdf)

## Enlaces

- **Repositorio Git:** <https://github.com/nievarodrigo/desarrollo-app-ii-tpo>
- **Tablero Trello:** <https://trello.com/b/gbrISl9W/desarrollo-de-aplicaciones-2-tpo>
- **Landing pública:** <https://landing-ecommerce-tau.vercel.app>
- **Prototipo del Catálogo:** [figma-checkpoint-1/README.md](figma-checkpoint-1/README.md)
- **Mockup y decisiones completas:** [MOCKUP_TPO.md](MOCKUP_TPO.md)

## Qué se entrega

- Ocho componentes candidatos identificados con interfaces y responsabilidades.
- Diagrama de arquitectura general y dependencias.
- Stack Jakarta EE 10, Java 17, Maven y WildFly justificado.
- Componente **Catálogo** implementado de punta a punta:
  - presentación responsive;
  - controllers JAX-RS;
  - servicio EJB stateless;
  - repositories detrás de interfaces;
  - DTO y manejo uniforme de errores.
- Doce pruebas unitarias.
- PDF técnico de cuatro páginas.
- Cuatro frames editables del Catálogo y exportaciones PNG.

## Verificación reproducible

### 1. Build limpio

```bash
cd TPO
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw clean test package
```

Resultado verificado el **04/09/2026**:

- `BUILD SUCCESS`
- 12 pruebas ejecutadas
- 0 fallas
- 0 errores
- WAR generado en `target/ecommerce.war`

### 2. Despliegue

```bash
WF=../wildfly/wildfly-41-ee10
cp target/ecommerce.war "$WF/standalone/deployments/"
"$WF/bin/standalone.sh"
```

Abrir:

- Aplicación: <http://localhost:8080/ecommerce/>
- Salud: <http://localhost:8080/ecommerce/api/salud>
- Productos: <http://localhost:8080/ecommerce/api/productos>
- Categorías: <http://localhost:8080/ecommerce/api/categorias>

### 3. Casos comprobados

| Caso | Resultado |
|---|---|
| `GET /api/salud` | HTTP 200, backend Jakarta EE disponible |
| `GET /api/categorias` | HTTP 200, 5 categorías |
| búsqueda `q=notebook&soloStock=true` | HTTP 200, 3 productos |
| `GET /api/productos/1` | HTTP 200, detalle completo |
| `GET /api/productos/999` | HTTP 404 con JSON controlado y sin error EJB en el log |
| `GET /ecommerce/` | HTTP 200, HTML de TPO Store |

## Checklist final

- [x] Componentes e interfaces documentados.
- [x] Arquitectura y dependencias documentadas.
- [x] Stack justificado.
- [x] Catálogo implementado en tres capas.
- [x] Carácter stateless justificado.
- [x] Build limpio y pruebas reproducibles.
- [x] Despliegue y API verificados en WildFly.
- [x] Documento técnico ajustado a cuatro páginas.
- [x] Tablero y landing enlazados.
- [ ] Agregar al docente y al resto del equipo como colaboradores del repositorio privado.
- [ ] Importar los SVG en Figma y pegar el enlace público en `figma-checkpoint-1/README.md`.
- [ ] Subir el PDF y los enlaces al aula virtual.

## Uso de IA

Se utilizó IA para asistencia de boilerplate, revisión de arquitectura, pruebas,
documentación y prototipado. Todo el material debe ser revisado y defendido por
los integrantes del equipo.

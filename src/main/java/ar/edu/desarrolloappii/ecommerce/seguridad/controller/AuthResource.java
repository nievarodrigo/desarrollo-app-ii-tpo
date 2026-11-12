package ar.edu.desarrolloappii.ecommerce.seguridad.controller;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Identidad;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Usuario;
import ar.edu.desarrolloappii.ecommerce.seguridad.service.AuthService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.time.Instant;
import java.util.List;

/** {@code /api/auth/*}: registro, ingreso (devuelve el JWT) y "quién soy". */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject AuthService auth;
    @Context SecurityContext seguridad;

    /** Cuerpos de entrada. Registro NO tiene campo rol: aunque lo manden, se ignora. */
    public static class Credenciales {
        private String email;
        private String clave;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getClave() { return clave; }
        public void setClave(String clave) { this.clave = clave; }
    }

    public static class DatosRegistro {
        private String nombre;
        private String email;
        private String clave;
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getClave() { return clave; }
        public void setClave(String clave) { this.clave = clave; }
    }

    /** Cuenta sin hash ni sal: sólo los 4 datos mínimos (SEC-10). */
    public record CuentaResponse(String nombre, String email, String rol, Instant creadoEn) {
        static CuentaResponse desde(Usuario u) {
            return new CuentaResponse(u.nombre(), u.email(), u.rol().name(), u.creadoEn());
        }
    }
    public record TokenResponse(String token, String tipo, long expiraEnSegundos, CuentaResponse usuario) { }

    @POST
    @Path("/login")
    public TokenResponse login(Credenciales c) {
        if (c == null) throw new IllegalArgumentException("Falta el cuerpo con email y clave.");
        AuthService.Sesion sesion = auth.ingresar(c.getEmail(), c.getClave());
        return new TokenResponse(sesion.token(), "Bearer", sesion.expiraEnSegundos(), CuentaResponse.desde(sesion.usuario()));
    }

    @POST
    @Path("/registro")
    public Response registro(DatosRegistro d) {
        if (d == null) throw new IllegalArgumentException("Falta el cuerpo con nombre, email y clave.");
        return Response.status(Response.Status.CREATED)
                .entity(CuentaResponse.desde(auth.registrar(d.getNombre(), d.getEmail(), d.getClave()))).build();
    }

    @GET
    @Path("/yo")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public Identidad yo() {
        return AutenticacionFilter.identidadDe(seguridad);
    }

    /** Sólo ADMIN: listado de cuentas. */
    @GET
    @Path("/usuarios")
    @RolesAllowed("ADMIN")
    public List<CuentaResponse> usuarios() {
        return auth.listarUsuarios().stream().map(CuentaResponse::desde).toList();
    }
}

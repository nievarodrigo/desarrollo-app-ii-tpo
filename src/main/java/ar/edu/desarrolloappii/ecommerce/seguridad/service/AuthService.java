package ar.edu.desarrolloappii.ecommerce.seguridad.service;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.ConflictoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.NoAutenticadoException;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Usuario;
import ar.edu.desarrolloappii.ecommerce.seguridad.repository.UsuarioRepository;
import ar.edu.desarrolloappii.ecommerce.shared.exception.DatosInvalidosException;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/** Registro e ingreso. Es el único lugar que conoce claves, hashes y el límite de intentos. */
@ApplicationScoped
public class AuthService {
    static final int CLAVE_MINIMA = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** Resultado de un ingreso exitoso. */
    public record Sesion(String token, long expiraEnSegundos, Usuario usuario) { }

    private UsuarioRepository usuarios;
    private PasswordHasher hasher;
    private JwtService jwt;
    private LimiteDeIntentos limite;
    private Clock reloj;
    /** Hash de relleno: se verifica contra él cuando el email no existe, para tardar lo mismo. */
    private String hashFalso;

    protected AuthService() { } // para el proxy de CDI

    @Inject
    public AuthService(UsuarioRepository usuarios, PasswordHasher hasher, JwtService jwt) {
        this(usuarios, hasher, jwt, Clock.systemUTC());
    }

    public AuthService(UsuarioRepository usuarios, PasswordHasher hasher, JwtService jwt, Clock reloj) {
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.jwt = jwt;
        this.reloj = reloj;
        this.limite = new LimiteDeIntentos(reloj);
        this.hashFalso = hasher.hashear("relleno-para-igualar-tiempos");
    }

    public Sesion ingresar(String email, String clave) {
        String id = email == null ? "" : email.trim().toLowerCase();
        limite.verificarDisponible(id);
        Usuario usuario = usuarios.buscarPorEmail(id).orElse(null);
        // Mismo mensaje y mismo trabajo (un hash) exista o no el email: SEC-02.
        boolean ok = hasher.verificar(clave == null ? "" : clave, usuario == null ? hashFalso : usuario.claveHash());
        if (usuario == null || !ok) {
            limite.registrarFallo(id);
            throw new NoAutenticadoException("Correo o contraseña incorrectos.");
        }
        limite.registrarExito(id);
        return new Sesion(jwt.emitir(usuario.identidad()), jwt.segundosDeVida(), usuario);
    }

    /** El rol NUNCA se recibe del cliente: toda cuenta nueva es CLIENTE (SEC-04). */
    public Usuario registrar(String nombre, String email, String clave) {
        String correo = email == null ? "" : email.trim().toLowerCase();
        if (nombre == null || nombre.isBlank() || nombre.length() > 80) {
            throw new DatosInvalidosException("El nombre es obligatorio (hasta 80 caracteres).");
        }
        if (!EMAIL.matcher(correo).matches() || correo.length() > 120) {
            throw new DatosInvalidosException("El correo no es válido.");
        }
        if (clave == null || clave.length() < CLAVE_MINIMA || clave.length() > 128) {
            throw new DatosInvalidosException("La contraseña necesita entre " + CLAVE_MINIMA + " y 128 caracteres.");
        }
        Usuario nuevo = new Usuario(nombre.trim(), correo, Rol.CLIENTE, hasher.hashear(clave), Instant.now(reloj));
        if (!usuarios.guardarSiNoExiste(nuevo)) throw new ConflictoException("Ya existe una cuenta con ese correo.");
        return nuevo;
    }

    public List<Usuario> listarUsuarios() { return usuarios.listar(); }
}

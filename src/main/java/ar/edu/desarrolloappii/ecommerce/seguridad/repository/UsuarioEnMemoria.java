package ar.edu.desarrolloappii.ecommerce.seguridad.repository;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Rol;
import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Usuario;
import ar.edu.desarrolloappii.ecommerce.seguridad.service.PasswordHasher;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Usuarios de la demo en memoria, con dos cuentas semilla (las mismas del front).
 * Las claves de la semilla son SÓLO para demostración y se guardan hasheadas.
 */
@ApplicationScoped
public class UsuarioEnMemoria implements UsuarioRepository {
    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();

    protected UsuarioEnMemoria() { } // para el proxy de CDI

    @Inject
    public UsuarioEnMemoria(PasswordHasher hasher) {
        sembrar(hasher, "Ana Gómez", "cliente@gomarket.com.ar", "cliente123", Rol.CLIENTE);
        sembrar(hasher, "Rodrigo Nieva", "admin@gomarket.com.ar", "admin123", Rol.ADMIN);
    }

    private void sembrar(PasswordHasher hasher, String nombre, String email, String clave, Rol rol) {
        guardarSiNoExiste(new Usuario(nombre, email, rol, hasher.hashear(clave), Instant.now()));
    }

    @Override public Optional<Usuario> buscarPorEmail(String email) {
        return Optional.ofNullable(usuarios.get(normalizar(email)));
    }

    @Override public boolean guardarSiNoExiste(Usuario usuario) {
        return usuarios.putIfAbsent(normalizar(usuario.email()), usuario) == null;
    }

    @Override public List<Usuario> listar() { return List.copyOf(usuarios.values()); }

    static String normalizar(String email) { return email == null ? "" : email.trim().toLowerCase(); }
}

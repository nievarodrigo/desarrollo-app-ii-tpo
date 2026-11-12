package ar.edu.desarrolloappii.ecommerce.seguridad.repository;

import ar.edu.desarrolloappii.ecommerce.seguridad.domain.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorEmail(String email);

    /** @return false si ya existía ese email (no pisa la cuenta). */
    boolean guardarSiNoExiste(Usuario usuario);

    List<Usuario> listar();
}

package org.example.lib;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import org.example.model.UsuarioEty;

// Jakarta Data: sin implementacion escrita a mano (no hay ejb/UsuarioRepositoryImpl --
// de hecho ya no hace falta ni el archivo, lo borre; estaba vacio de todas formas).
@Repository
public interface UsuarioRepository extends CrudRepository<UsuarioEty, Long> {
}

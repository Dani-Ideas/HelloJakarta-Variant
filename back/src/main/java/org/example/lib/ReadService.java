package org.example.lib;

import java.util.List;

// Solo las operaciones de LECTURA -- ningun XService que solo necesite consultar datos
// deberia depender de metodos de escritura que nunca va a usar (principio de segregacion
// de interfaces). Cualquier XService que exponga GET extiende esta.
public interface ReadService<D, ID> extends Service<D, ID> {

    List<D> listar();

    D buscarPorId(ID id);
}

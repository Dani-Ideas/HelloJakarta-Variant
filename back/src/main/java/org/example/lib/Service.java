package org.example.lib;

import java.util.List;

// Igual que Repository<T,ID>, pero para la capa de negocio: D = tipo de DTO. Solo trae
// las 3 operaciones que SIEMPRE tiene sentido exponer para cualquier entidad (crear,
// listar, buscar). "actualizar"/"eliminar" quedan fuera del generico a propósito -- no
// todas las entidades los necesitan (Factura no los expone).
public interface Service<D, ID> {

    D crear(D dto);

    List<D> listar();

    D buscarPorId(ID id);
}

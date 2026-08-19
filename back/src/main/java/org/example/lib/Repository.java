package org.example.lib;

import java.util.List;

// Contrato generico para acceso a datos. T = tipo de entidad JPA (Producto, Factura),
// ID = tipo de la llave primaria. Cualquiera que dependa de un Repository solo conoce
// esta interfaz, nunca la clase concreta que la implementa -- eso es lo que permite
// inyectar "la implementacion que sea" sin acoplarse a ella (polimorfismo).
public interface Repository<T, ID> {

    T crear(T entidad);

    List<T> listar();

    T buscarPorId(ID id);

    boolean eliminar(ID id);
}

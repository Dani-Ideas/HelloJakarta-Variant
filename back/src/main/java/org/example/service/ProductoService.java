package org.example.service;

import jakarta.ejb.Stateless;
import org.example.model.Producto;

@Stateless
public class ProductoService extends CrudService<Producto, Long> {

    @Override
    protected Class<Producto> getEntityClass() {
        return Producto.class;
    }

    public Producto actualizar(Long id, Producto cambios) {
        return actualizar(id, cambios, (entidad, c) -> {
            entidad.setNombre(c.getNombre());
            entidad.setSku(c.getSku());
            entidad.setPrecio(c.getPrecio());
            entidad.setStock(c.getStock());
        });
    }
}

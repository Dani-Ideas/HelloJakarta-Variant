package org.example.ejb;

import jakarta.ejb.Stateless;
import org.example.lib.ProductoRepository;
import org.example.model.Producto;

// @Stateless es lo que registra esta clase como bean ante GlassFish. Nadie mas en el
// proyecto conoce este nombre de clase -- todos inyectan la interfaz ProductoRepository
// (de org.example.lib), y GlassFish resuelve automaticamente que ESTA es la unica
// implementacion disponible.
@Stateless
public class ProductoRepositoryImpl extends AbstractRepository<Producto, Long> implements ProductoRepository {

    @Override
    protected Class<Producto> getEntityClass() {
        return Producto.class;
    }

    @Override
    public Producto actualizar(Long id, Producto cambios) {
        return aplicarCambios(id, cambios, (entidad, c) -> {
            entidad.setNombre(c.getNombre());
            entidad.setSku(c.getSku());
            entidad.setPrecio(c.getPrecio());
            entidad.setStock(c.getStock());
        });
    }
}

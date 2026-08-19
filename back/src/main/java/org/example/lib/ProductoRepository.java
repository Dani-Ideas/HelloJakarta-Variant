package org.example.lib;

import org.example.model.Producto;

// Extiende el Repository generico con lo que le es propio a Producto: "actualizar" no
// puede ser generico porque copiar campos SI depende de la entidad (nombre, sku, precio,
// stock son especificos de Producto, no existen en Factura).
public interface ProductoRepository extends Repository<Producto, Long> {

    Producto actualizar(Long id, Producto cambios);
}

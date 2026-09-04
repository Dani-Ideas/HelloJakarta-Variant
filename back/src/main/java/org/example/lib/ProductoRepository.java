package org.example.lib;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import org.example.model.ProductoEty;

// PILOTO Jakarta Data: esta interfaz ya NO tiene implementacion escrita a mano -- no
// existe (ni debe existir) un "ProductoRepositoryImpl" en ejb/. El proveedor (EclipseLink,
// via GlassFish 8) genera la clase real en tiempo de despliegue a partir de este contrato.
// Por eso el "actualizar" con copiado de campos que antes vivia en el Repository (via
// AbstractRepository.aplicarCambios) se movio a ProductoServiceImpl -- ya no hay donde
// escribir ese codigo aqui, y de hecho es logica de negocio, no de acceso a datos.
@Repository
public interface ProductoRepository extends CrudRepository<ProductoEty, Long> {
}
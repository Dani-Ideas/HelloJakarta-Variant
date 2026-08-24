package org.example.lib;

import org.example.model.Factura;

// Factura SI expone actualizar (corregir datos de encabezado como cliente/fecha), pero
// deliberadamente NO expone eliminar por REST -- borrar una factura ya emitida no tiene
// sentido de negocio real, por eso Repository.eliminar() nunca se usa desde el Resource
// para esta entidad aunque el metodo generico exista.
public interface FacturaRepository extends Repository<Factura, Long> {

    Factura actualizar(Long id, Factura cambios);
}

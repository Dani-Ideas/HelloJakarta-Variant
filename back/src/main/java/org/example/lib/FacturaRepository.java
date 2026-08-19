package org.example.lib;

import org.example.model.Factura;

// Factura no expone actualizar/eliminar por REST (no se editan/borran facturas ya
// emitidas en un sistema real) -- por eso este interfaz no agrega nada al generico,
// crear/listar/buscarPorId le alcanzan tal cual.
public interface FacturaRepository extends Repository<Factura, Long> {
}

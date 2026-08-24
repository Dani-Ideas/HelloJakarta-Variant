package org.example.ejb;

import jakarta.ejb.Stateless;
import org.example.lib.FacturaRepository;
import org.example.model.Factura;

// crear/listar/buscarPorId genericos de AbstractRepository le alcanzan tal cual (el
// calculo de precios/total de una factura NO va aqui, es logica de negocio, vive en
// FacturaServiceImpl -- este Repository solo persiste, sin decidir nada).
@Stateless
public class FacturaRepositoryImpl extends AbstractRepository<Factura, Long> implements FacturaRepository {

    @Override
    protected Class<Factura> getEntityClass() {
        return Factura.class;
    }

    @Override
    public Factura actualizar(Long id, Factura cambios) {
        return aplicarCambios(id, cambios, (entidad, c) -> {
            entidad.setNumero(c.getNumero());
            entidad.setFecha(c.getFecha());
            entidad.setCliente(c.getCliente());
            // detalles y total NO se tocan aqui a proposito: editar las lineas de una
            // factura ya emitida (agregar/quitar productos, recalcular total) es una
            // operacion mas compleja que queda fuera del alcance de este PUT -- aqui solo
            // se corrigen datos de encabezado (numero, fecha, cliente).
        });
    }
}

package org.example.ejb;

import jakarta.ejb.Stateless;
import org.example.lib.FacturaRepository;
import org.example.model.Factura;

// No necesita nada propio -- crear/listar/buscarPorId genericos de AbstractRepository le
// alcanzan tal cual (el calculo de precios/total de una factura NO va aqui, es logica de
// negocio, vive en FacturaServiceImpl -- este Repository solo persiste, sin decidir nada).
@Stateless
public class FacturaRepositoryImpl extends AbstractRepository<Factura, Long> implements FacturaRepository {

    @Override
    protected Class<Factura> getEntityClass() {
        return Factura.class;
    }
}

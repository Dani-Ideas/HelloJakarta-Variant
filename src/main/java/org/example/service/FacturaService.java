package org.example.service;

import jakarta.ejb.Stateless;
import org.example.model.Factura;
import org.example.model.FacturaDetalle;
import org.example.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDate;

@Stateless
public class FacturaService extends CrudService<Factura, Long> {

    @Override
    protected Class<Factura> getEntityClass() {
        return Factura.class;
    }

    @Override
    public Factura crear(Factura factura) {
        if (factura.getFecha() == null) {
            factura.setFecha(LocalDate.now());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (FacturaDetalle detalle : factura.getDetalles()) {
            // El precio SIEMPRE se recalcula del lado del servidor, nunca se confia
            // en el precio que mande el cliente en el JSON.
            Producto producto = em.find(Producto.class, detalle.getProducto().getId());
            detalle.setProducto(producto);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            detalle.setFactura(factura);
            total = total.add(detalle.getSubtotal());
        }
        factura.setTotal(total);

        em.persist(factura);
        return factura;
    }
}

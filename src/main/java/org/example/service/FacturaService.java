package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.Factura;
import org.example.model.FacturaDetalle;
import org.example.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Stateless
public class FacturaService {

    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

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

    public List<Factura> listar() {
        return em.createQuery("SELECT f FROM Factura f ORDER BY f.id", Factura.class)
                .getResultList();
    }

    public Factura buscarPorId(Long id) {
        return em.find(Factura.class, id);
    }
}

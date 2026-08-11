package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.Producto;

import java.util.List;

@Stateless
public class ProductoService {

    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

    public Producto crear(Producto producto) {
        em.persist(producto);
        return producto;
    }

    public List<Producto> listar() {
        return em.createQuery("SELECT p FROM Producto p ORDER BY p.id", Producto.class)
                .getResultList();
    }

    public Producto buscarPorId(Long id) {
        return em.find(Producto.class, id);
    }
}

package org.example.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.function.BiConsumer;

// Clase base generica con las operaciones CRUD comunes a cualquier entidad.
// Las subclases (@Stateless) solo indican de que entidad se trata via getEntityClass(),
// y sobrescriben lo que sea especifico de su negocio (ej. FacturaService.crear()).
public abstract class CrudService<T, ID> {

    @PersistenceContext(unitName = "HelloJakartaPU")
    protected EntityManager em;

    protected abstract Class<T> getEntityClass();

    public T crear(T entidad) {
        em.persist(entidad);
        return entidad;
    }

    public List<T> listar() {
        String jpql = "SELECT e FROM " + getEntityClass().getSimpleName() + " e ORDER BY e.id";
        return em.createQuery(jpql, getEntityClass()).getResultList();
    }

    public T buscarPorId(ID id) {
        return em.find(getEntityClass(), id);
    }

    // El "como se copian los campos" varia por entidad, por eso se recibe como parametro
    // en vez de intentar adivinarlo con reflexion.
    public T actualizar(ID id, T cambios, BiConsumer<T, T> aplicarCambios) {
        T entidad = buscarPorId(id);
        if (entidad == null) {
            return null;
        }
        aplicarCambios.accept(entidad, cambios);
        return entidad;
    }

    public boolean eliminar(ID id) {
        T entidad = buscarPorId(id);
        if (entidad == null) {
            return false;
        }
        em.remove(entidad);
        return true;
    }
}

package org.example.ejb;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.lib.Repository;

import java.util.List;
import java.util.function.BiConsumer;

// Implementacion compartida de las operaciones genericas de Repository<T,ID>. No es un
// bean por si sola (no tiene @Stateless) -- son los hijos concretos (ProductoRepositoryImpl,
// FacturaRepositoryImpl) los que se registran como EJB, heredando este codigo.
public abstract class AbstractRepository<T, ID> implements Repository<T, ID> {

    @PersistenceContext(unitName = "HelloJakartaPU")
    protected EntityManager em;

    protected abstract Class<T> getEntityClass();

    @Override
    public T crear(T entidad) {
        em.persist(entidad);
        // Ahora que crear() se llama a traves de una interfaz inyectada en OTRO bean
        // (Service -> Repository, en vez de una llamada directa dentro de la misma
        // clase como antes), el id generado por IDENTITY no queda sincronizado en el
        // objeto a tiempo para el "return" sin forzar el INSERT real con flush().
        em.flush();
        return entidad;
    }

    @Override
    public List<T> listar() {
        String jpql = "SELECT e FROM " + getEntityClass().getSimpleName() + " e ORDER BY e.id";
        return em.createQuery(jpql, getEntityClass()).getResultList();
    }

    @Override
    public T buscarPorId(ID id) {
        return em.find(getEntityClass(), id);
    }

    @Override
    public boolean eliminar(ID id) {
        T entidad = buscarPorId(id);
        if (entidad == null) {
            return false;
        }
        em.remove(entidad);
        return true;
    }

    // Helper para que los hijos implementen su "actualizar" propio (declarado en su
    // interfaz especifica, ej. ProductoRepository) sin repetir el find+null-check.
    protected T aplicarCambios(ID id, T cambios, BiConsumer<T, T> copiador) {
        T entidad = buscarPorId(id);
        if (entidad == null) {
            return null;
        }
        copiador.accept(entidad, cambios);
        return entidad;
    }
}

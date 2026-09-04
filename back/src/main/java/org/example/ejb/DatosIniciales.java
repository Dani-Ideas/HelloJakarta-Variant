package org.example.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.example.lib.ProductoRepository;
import org.example.model.ProductoEty;

import java.math.BigDecimal;

@Singleton
@Startup
public class DatosIniciales {

    @Inject
    private ProductoRepository productoRepository;

    @PostConstruct
    public void cargarDatos() {
        // findAll() devuelve Stream<T>, no List<T> -- findAny().isEmpty() es el
        // equivalente correcto de "no hay ningun producto todavia".
        if (productoRepository.findAll().findAny().isEmpty()) {
            productoRepository.insert(new ProductoEty("Cuaderno profesional", "PRD-001", new BigDecimal("45.00"), 120));
            productoRepository.insert(new ProductoEty("Boligrafo tinta negra", "PRD-002", new BigDecimal("8.50"), 300));
            productoRepository.insert(new ProductoEty("Calculadora cientifica", "PRD-003", new BigDecimal("250.00"), 40));
        }
    }
}

package org.example.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.example.lib.ProductoRepository;
import org.example.model.Producto;

import java.math.BigDecimal;

@Singleton
@Startup
public class DatosIniciales {

    @EJB
    private ProductoRepository productoRepository;

    @PostConstruct
    public void cargarDatos() {
        if (productoRepository.listar().isEmpty()) {
            productoRepository.crear(new Producto("Cuaderno profesional", "PRD-001", new BigDecimal("45.00"), 120));
            productoRepository.crear(new Producto("Boligrafo tinta negra", "PRD-002", new BigDecimal("8.50"), 300));
            productoRepository.crear(new Producto("Calculadora cientifica", "PRD-003", new BigDecimal("250.00"), 40));
        }
    }
}

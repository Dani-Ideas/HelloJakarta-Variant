package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dto.ProductoDto;
import org.example.dto.ProductoPatchDto;
import org.example.lib.ProductoReadService;
import org.example.lib.ProductoService;
import org.example.lib.ProductoWriteService;

import java.util.List;

// Fachada: sin logica propia. Decide a cual de los dos (Read o Write) le corresponde cada
// peticion, y delega -- el Resource sigue inyectando un solo Service (ProductoService),
// sin enterarse de que por dentro hay dos implementaciones distintas.
@Stateless
public class ProductoServiceImpl implements ProductoService {

    @EJB
    private ProductoReadService productoReadService;

    @EJB
    private ProductoWriteService productoWriteService;

    @Override
    public List<ProductoDto> listar() {
        return productoReadService.listar();
    }

    @Override
    public ProductoDto buscarPorId(Long id) {
        return productoReadService.buscarPorId(id);
    }

    @Override
    public ProductoDto crear(ProductoDto dto) {
        return productoWriteService.crear(dto);
    }

    @Override
    public ProductoDto actualizar(Long id, ProductoDto dto) {
        return productoWriteService.actualizar(id, dto);
    }

    @Override
    public ProductoDto patch(Long id, ProductoPatchDto cambios) {
        return productoWriteService.patch(id, cambios);
    }

    @Override
    public boolean eliminar(Long id) {
        return productoWriteService.eliminar(id);
    }

    // Delegacion "hueca" a proposito: el Controller nunca llama estos dos (no son parte de
    // la API HTTP), pero como se agregaron a la interfaz ProductoReadService, esta fachada
    // (que implementa ProductoService = ProductoReadService + ProductoWriteService) tiene
    // que implementarlos igual que el resto.
    @Override
    public void refrescarCache(ProductoDto dto) {
        productoReadService.refrescarCache(dto);
    }

    @Override
    public void quitarDeCache(Long id) {
        productoReadService.quitarDeCache(id);
    }
}

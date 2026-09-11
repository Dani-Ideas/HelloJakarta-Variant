package org.example.lib;

import org.example.dto.ProductoDto;

// refrescarCache/quitarDeCache NO son parte de la API publica -- el Controller nunca los
// llama (solo usa listar/buscarPorId, heredados de ReadService). Existen solo para que
// ProductoWriteServiceImpl le avise a este mismo bean (que ahora es @Singleton con cache
// en memoria) cuando cambia un Producto, y el cache no se quede desactualizado.
public interface ProductoReadService extends ReadService<ProductoDto, Long> {

    void refrescarCache(ProductoDto dto);

    void quitarDeCache(Long id);
}

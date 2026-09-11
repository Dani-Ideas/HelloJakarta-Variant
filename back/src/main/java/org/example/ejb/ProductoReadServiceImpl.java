package org.example.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.example.dto.ProductoDto;
import org.example.lib.ProductoReadService;
import org.example.lib.ProductoRepository;
import org.example.mapper.ProductoMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// @Singleton (ya NO @Stateless): Producto es una "tabla base" (sin FK saliente) que se
// escribe poco y se lee mucho, asi que se precarga UNA VEZ en memoria en vez de ir a la
// base de datos en cada listar()/buscarPorId(). @Startup + @DependsOn("DatosIniciales")
// fuerzan que la precarga ocurra al desplegar la app, y despues de que DatosIniciales ya
// haya sembrado las filas de ejemplo -- sin @DependsOn, este Singleton igual cargaria
// despues (el deploy no termina hasta que todos los @Startup terminan), pero se deja
// explicito para no depender de ese orden por casualidad.
//
// @Lock: un @Singleton por defecto pone @Lock(WRITE) en TODOS sus metodos -- exclusivo, un
// solo hilo a la vez, aunque solo este leyendo. Sin marcar @Lock(READ) en listar()/
// buscarPorId(), este cache seria MAS LENTO que sin cache (serializaria todas las lecturas
// una por una). refrescarCache/quitarDeCache si necesitan @Lock(WRITE): son las unicas que
// modifican el mapa.
@Singleton
@Startup
@DependsOn("DatosIniciales")
public class ProductoReadServiceImpl implements ProductoReadService {

    @Inject
    private ProductoRepository productoRepository;

    private final ProductoMapper productoMapper = ProductoMapper.INSTANCE;

    private final Map<Long, ProductoDto> cache = new ConcurrentHashMap<>();

    @PostConstruct
    private void precargarCache() {
        productoRepository.findAll()
                .map(productoMapper::toDto)
                .forEach(dto -> cache.put(dto.id(), dto));
    }

    @Override
    @Lock(LockType.READ)
    public List<ProductoDto> listar() {
        return List.copyOf(cache.values());
    }

    @Override
    @Lock(LockType.READ)
    public ProductoDto buscarPorId(Long id) {
        return cache.get(id);
    }

    @Override
    @Lock(LockType.WRITE)
    public void refrescarCache(ProductoDto dto) {
        cache.put(dto.id(), dto);
    }

    @Override
    @Lock(LockType.WRITE)
    public void quitarDeCache(Long id) {
        cache.remove(id);
    }
}

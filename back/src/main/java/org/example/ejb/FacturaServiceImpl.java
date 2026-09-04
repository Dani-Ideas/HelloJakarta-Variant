package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.FacturaDto;
import org.example.dto.FacturaPatchDto;
import org.example.lib.FacturaRepository;
import org.example.lib.FacturaService;
import org.example.lib.ProductoRepository;
import org.example.mapper.FacturaMapper;
import org.example.model.FacturaDetalleEty;
import org.example.model.FacturaEty;
import org.example.model.ProductoEty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
public class FacturaServiceImpl implements FacturaService {

    // @Inject, no @EJB: FacturaRepository es un repositorio Jakarta Data (bean CDI),
    // generado por el proveedor -- no existe FacturaRepositoryImpl escrito a mano.
    @Inject
    private FacturaRepository facturaRepository;

    // Necesita el repositorio de OTRA entidad para calcular precios reales -- esto es
    // justo el tipo de regla que pertenece a la capa de Service, no a un Repository
    // (que debe quedarse "tonto", solo persistiendo lo que se le pase).
    @Inject
    private ProductoRepository productoRepository;

    private final FacturaMapper facturaMapper = FacturaMapper.INSTANCE;

    @Override
    public FacturaDto crear(FacturaDto dto) {
        FacturaEty factura = facturaMapper.toEntity(dto);

        if (factura.getFecha() == null) {
            factura.setFecha(LocalDate.now());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (FacturaDetalleEty detalle : factura.getDetalles()) {
            // El precio SIEMPRE se recalcula del lado del servidor, nunca se confia en el
            // precio que mande el cliente en el JSON -- solo se usa el id del producto
            // referenciado, el resto de "producto" que haya mandado el cliente se ignora.
            ProductoEty producto = productoRepository.findById(detalle.getProducto().getId()).orElse(null);
            detalle.setProducto(producto);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            // Lado dueno de la relacion: FacturaMapper.toEntity() (via FacturaDetalleMapper)
            // ignora "factura" a proposito -- aqui es donde SI sabemos cual es la Factura
            // padre, hay que asignarla a mano para que la cascada persista bien.
            detalle.setFactura(factura);
            total = total.add(detalle.getSubtotal());
        }
        factura.setTotal(total);

        // Sin EntityManager/flush() aqui: Factura.id usa GenerationType.SEQUENCE, no
        // IDENTITY -- ver ProductoServiceImpl.crear().
        FacturaEty creada = facturaRepository.insert(factura);
        return facturaMapper.toDto(creada);
    }

    @Override
    public List<FacturaDto> listar() {
        // findAll() de Jakarta Data devuelve Stream<T>, no List<T>.
        return facturaRepository.findAll()
                .map(facturaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public FacturaDto buscarPorId(Long id) {
        return facturaMapper.toDto(facturaRepository.findById(id).orElse(null));
    }

    @Override
    public FacturaDto actualizar(Long id, FacturaDto dto) {
        // El copiado de campos vive aqui, no en el Repository -- es logica de negocio.
        // Solo se tocan numero/fecha/cliente: editar detalles/total de una factura ya
        // emitida queda fuera del alcance de este PUT.
        Optional<FacturaEty> existente = facturaRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        FacturaEty factura = existente.get();
        factura.setNumero(dto.numero());
        factura.setFecha(dto.fecha());
        factura.setCliente(dto.cliente());
        FacturaEty actualizada = facturaRepository.update(factura);
        return facturaMapper.toDto(actualizada);
    }

    @Override
    public FacturaDto patch(Long id, FacturaPatchDto cambios) {
        // Diferencia con actualizar() (PUT): el cliente manda solo el campo que quiere
        // corregir -- no hace falta reenviar numero/fecha/cliente/detalles completos.
        Optional<FacturaEty> existente = facturaRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        FacturaEty factura = existente.get();
        if (cambios.numero() != null) {
            factura.setNumero(cambios.numero());
        }
        if (cambios.fecha() != null) {
            factura.setFecha(cambios.fecha());
        }
        if (cambios.cliente() != null) {
            factura.setCliente(cambios.cliente());
        }
        FacturaEty actualizada = facturaRepository.update(factura);
        return facturaMapper.toDto(actualizada);
    }
}

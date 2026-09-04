package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.FacturaDto;
import org.example.dto.FacturaPatchDto;
import org.example.lib.FacturaRepository;
import org.example.lib.FacturaWriteService;
import org.example.lib.ProductoRepository;
import org.example.mapper.FacturaMapper;
import org.example.model.FacturaDetalleEty;
import org.example.model.FacturaEty;
import org.example.model.ProductoEty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Stateless
public class FacturaWriteServiceImpl implements FacturaWriteService {

    @Inject
    private FacturaRepository facturaRepository;

    // Necesita el repositorio de OTRA entidad para calcular precios reales -- logica de
    // negocio, pertenece aqui, no al Repository.
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
            // El precio SIEMPRE se recalcula del lado del servidor.
            ProductoEty producto = productoRepository.findById(detalle.getProducto().getId()).orElse(null);
            detalle.setProducto(producto);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            detalle.setFactura(factura);
            total = total.add(detalle.getSubtotal());
        }
        factura.setTotal(total);

        // Sin EntityManager/flush(): Factura.id usa SEQUENCE, no IDENTITY.
        FacturaEty creada = facturaRepository.insert(factura);
        return facturaMapper.toDto(creada);
    }

    @Override
    public FacturaDto actualizar(Long id, FacturaDto dto) {
        Optional<FacturaEty> existente = facturaRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        FacturaEty factura = existente.get();
        factura.setNumero(dto.numero());
        factura.setFecha(dto.fecha());
        factura.setCliente(dto.cliente());
        return facturaMapper.toDto(facturaRepository.update(factura));
    }

    @Override
    public FacturaDto patch(Long id, FacturaPatchDto cambios) {
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
        return facturaMapper.toDto(facturaRepository.update(factura));
    }
}

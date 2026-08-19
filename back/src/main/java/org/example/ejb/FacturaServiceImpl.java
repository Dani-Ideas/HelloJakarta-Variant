package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dto.FacturaDTO;
import org.example.lib.FacturaRepository;
import org.example.lib.FacturaService;
import org.example.lib.ProductoRepository;
import org.example.mapper.FacturaMapper;
import org.example.model.Factura;
import org.example.model.FacturaDetalle;
import org.example.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class FacturaServiceImpl implements FacturaService {

    @EJB
    private FacturaRepository facturaRepository;

    // Necesita el repositorio de OTRA entidad para calcular precios reales -- esto es
    // justo el tipo de regla que pertenece a la capa de Service, no a un Repository
    // (que debe quedarse "tonto", solo persistiendo lo que se le pase).
    @EJB
    private ProductoRepository productoRepository;

    @Override
    public FacturaDTO crear(FacturaDTO dto) {
        Factura factura = FacturaMapper.toEntity(dto);

        if (factura.getFecha() == null) {
            factura.setFecha(LocalDate.now());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (FacturaDetalle detalle : factura.getDetalles()) {
            // El precio SIEMPRE se recalcula del lado del servidor, nunca se confia
            // en el precio que mande el cliente en el JSON.
            Producto producto = productoRepository.buscarPorId(detalle.getProducto().getId());
            detalle.setProducto(producto);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            detalle.setFactura(factura);
            total = total.add(detalle.getSubtotal());
        }
        factura.setTotal(total);

        Factura creada = facturaRepository.crear(factura);
        return FacturaMapper.toDTO(creada);
    }

    @Override
    public List<FacturaDTO> listar() {
        return facturaRepository.listar().stream()
                .map(FacturaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FacturaDTO buscarPorId(Long id) {
        return FacturaMapper.toDTO(facturaRepository.buscarPorId(id));
    }
}

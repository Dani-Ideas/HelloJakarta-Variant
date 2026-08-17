package org.example.mapper;

import org.example.dto.FacturaDTO;
import org.example.dto.FacturaDetalleDTO;
import org.example.model.Factura;
import org.example.model.FacturaDetalle;
import org.example.model.Producto;

import java.util.List;
import java.util.stream.Collectors;

public final class FacturaMapper {

    private FacturaMapper() {
    }

    public static FacturaDTO toDTO(Factura factura) {
        if (factura == null) {
            return null;
        }

        FacturaDTO dto = new FacturaDTO();
        dto.setId(factura.getId());
        dto.setNumero(factura.getNumero());
        dto.setFecha(factura.getFecha());
        dto.setCliente(factura.getCliente());
        dto.setTotal(factura.getTotal());
        dto.setDetalles(factura.getDetalles().stream()
                .map(FacturaMapper::toDetalleDTO)
                .collect(Collectors.toList()));
        return dto;
    }

    private static FacturaDetalleDTO toDetalleDTO(FacturaDetalle detalle) {
        FacturaDetalleDTO dto = new FacturaDetalleDTO();
        dto.setId(detalle.getId());
        dto.setProductoId(detalle.getProducto().getId());
        dto.setNombreProducto(detalle.getProducto().getNombre());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setSubtotal(detalle.getSubtotal());
        return dto;
    }

    public static Factura toEntity(FacturaDTO dto) {
        Factura factura = new Factura();
        factura.setNumero(dto.getNumero());
        factura.setFecha(dto.getFecha());
        factura.setCliente(dto.getCliente());

        List<FacturaDetalle> detalles = dto.getDetalles().stream()
                .map(FacturaMapper::toDetalleEntity)
                .collect(Collectors.toList());
        factura.setDetalles(detalles);

        return factura;
    }

    private static FacturaDetalle toDetalleEntity(FacturaDetalleDTO dto) {
        FacturaDetalle detalle = new FacturaDetalle();
        Producto producto = new Producto();
        producto.setId(dto.getProductoId());
        detalle.setProducto(producto);
        detalle.setCantidad(dto.getCantidad());
        return detalle;
    }
}

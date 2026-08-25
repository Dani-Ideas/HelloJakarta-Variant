package org.example.mapper;

import org.example.dto.FacturaDTO;
import org.example.dto.FacturaDetalleDTO;
import org.example.model.Factura;
import org.example.model.FacturaDetalle;
import org.example.model.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

// MapStruct: sin implementacion escrita a mano (MapStruct genera FacturaMapperImpl). Los
// 4 metodos de abajo se completan solos donde el nombre/tipo coincide; donde no coincide
// (productoId <-> producto, o campos que se ignoran a proposito) se declara con @Mapping.
@Mapper
public interface FacturaMapper {

    FacturaMapper INSTANCE = Mappers.getMapper(FacturaMapper.class);

    // detalles: List<FacturaDetalle> -> List<FacturaDetalleDTO> se resuelve solo -- MapStruct
    // ve que toDetalleDTO() convierte un FacturaDetalle en FacturaDetalleDTO y lo aplica
    // automaticamente a cada elemento de la lista.
    FacturaDTO toDTO(Factura factura);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "nombreProducto", source = "producto.nombre")
    FacturaDetalleDTO toDetalleDTO(FacturaDetalle detalle);

    // id y sesionCaja se ignoran: una Factura nueva no nace con el id del cliente (lo
    // genera la base), y sesionCaja no existe en FacturaDTO -- no hay de donde mapearlo,
    // se asigna aparte si hiciera falta.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sesionCaja", ignore = true)
    Factura toEntity(FacturaDTO dto);

    // productoId (Long) -> producto (Producto): no son el mismo tipo, MapStruct necesita
    // ayuda -- usa el metodo "map(Long)" de abajo automaticamente porque el tipo de
    // retorno (Producto) coincide con el target. precioUnitario/subtotal se ignoran porque
    // se calculan en el servidor (FacturaServiceImpl.crear), nunca vienen del cliente.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", source = "productoId")
    @Mapping(target = "precioUnitario", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "factura", ignore = true)
    FacturaDetalle toDetalleEntity(FacturaDetalleDTO dto);

    default Producto map(Long productoId) {
        if (productoId == null) {
            return null;
        }
        Producto producto = new Producto();
        producto.setId(productoId);
        return producto;
    }
}

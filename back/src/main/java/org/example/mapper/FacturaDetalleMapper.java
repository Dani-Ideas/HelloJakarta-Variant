package org.example.mapper;

import org.example.dto.FacturaDetalleDto;
import org.example.dto.ProductoDto;
import org.example.model.FacturaDetalleEty;
import org.example.model.ProductoEty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

// uses = ProductoMapper: para el "toDto" (FacturaDetalleEty.producto -> DTO), donde SI se
// quiere el Producto completo. Para "toEntity" NO se puede reusar ProductoMapper.toEntity()
// -- ese metodo ignora el id a proposito (es para crear un Producto nuevo), pero aqui el
// caso es al reves: el cliente esta REFERENCIANDO un producto que ya existe por su id
// (ej. {"producto": {"id": 1}, "cantidad": 2}). Usar el mapeo equivocado causaba
// "NullPointerException: id is required" al buscar el producto en FacturaServiceImpl.
@Mapper(uses = ProductoMapper.class)
public interface FacturaDetalleMapper {

    FacturaDetalleMapper INSTANCE = Mappers.getMapper(FacturaDetalleMapper.class);

    FacturaDetalleDto toDto(FacturaDetalleEty detalle);

    // id y factura se ignoran: el id lo genera la base, y "factura" (el lado dueno de la
    // relacion) se asigna a mano en FacturaServiceImpl.crear().
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "factura", ignore = true)
    @Mapping(target = "producto", qualifiedByName = "referenciaProducto")
    FacturaDetalleEty toEntity(FacturaDetalleDto dto);

    // Mapeo "por referencia": solo copia el id, no crea un Producto nuevo -- lo distingue
    // de ProductoMapper.toEntity() (que si ignora el id, para crear).
    @Named("referenciaProducto")
    default ProductoEty referenciaProducto(ProductoDto dto) {
        if (dto == null) {
            return null;
        }
        ProductoEty producto = new ProductoEty();
        producto.setId(dto.id());
        return producto;
    }
}

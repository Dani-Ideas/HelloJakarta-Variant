package org.example.mapper;

import org.example.dto.ProductoDTO;
import org.example.model.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

// MapStruct: esta interfaz ya NO tiene implementacion escrita a mano -- MapStruct genera
// ProductoMapperImpl en tiempo de compilacion (revisa target/generated-sources/annotations
// despues de compilar si quieres ver el codigo real que arma). Mismo espiritu que Jakarta
// Data con el Repository: uno escribe el contrato, la herramienta escribe el cuerpo.
// nombre/sku/precio/stock coinciden de nombre y tipo entre Producto y ProductoDTO, por eso
// MapStruct los mapea solo sin que haga falta declarar nada para esos 4 campos.
@Mapper
public interface ProductoMapper {

    ProductoMapper INSTANCE = Mappers.getMapper(ProductoMapper.class);

    ProductoDTO toDTO(Producto producto);

    // id se ignora a proposito: un Producto nuevo nunca debe nacer con el id que (si
    // acaso) mando el cliente en el JSON -- lo genera la base de datos (IDENTITY).
    @Mapping(target = "id", ignore = true)
    Producto toEntity(ProductoDTO dto);
}

package org.example.rest;

import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dto.ProductoDTO;
import org.example.mapper.ProductoMapper;
import org.example.model.Producto;
import org.example.service.ProductoService;

import java.util.List;
import java.util.stream.Collectors;

@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoResource {

    @EJB
    private ProductoService productoService;

    @GET
    public List<ProductoDTO> listar() {
        return productoService.listar().stream()
                .map(ProductoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        Producto producto = productoService.buscarPorId(id);
        if (producto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(ProductoMapper.toDTO(producto)).build();
    }

    @POST
    public Response crear(@Valid ProductoDTO dto) {
        Producto producto = productoService.crear(ProductoMapper.toEntity(dto));
        return Response.status(Response.Status.CREATED).entity(ProductoMapper.toDTO(producto)).build();
    }
}

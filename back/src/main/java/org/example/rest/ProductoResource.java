package org.example.rest;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dto.ProductoDTO;
import org.example.lib.ProductoService;

import java.util.List;
import java.util.Map;

// El Resource ya NO mapea Entity<->DTO -- eso ahora vive en ProductoServiceImpl. Aqui
// solo se traducen llamadas HTTP a llamadas de metodo, y resultados de metodo a
// respuestas HTTP (codigos de estado). Tambien inyecta la INTERFAZ (org.example.lib),
// nunca ProductoServiceImpl directamente.
@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoResource {

    @EJB
    private ProductoService productoService;

    @GET
    public List<ProductoDTO> listar() {
        return productoService.listar();
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        ProductoDTO dto = productoService.buscarPorId(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @POST
    public Response crear(@Valid ProductoDTO dto) {
        ProductoDTO creado = productoService.crear(dto);
        return Response.status(Response.Status.CREATED).entity(creado).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") Long id, @Valid ProductoDTO dto) {
        ProductoDTO actualizado = productoService.actualizar(id, dto);
        if (actualizado == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(actualizado).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        try {
            boolean eliminado = productoService.eliminar(id);
            if (!eliminado) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.noContent().build();
        } catch (EJBException e) {
            // Ocurre cuando el producto esta referenciado por una FacturaDetalle (llave foranea)
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "No se puede eliminar: el producto esta siendo usado en una o mas facturas"))
                    .build();
        }
    }
}

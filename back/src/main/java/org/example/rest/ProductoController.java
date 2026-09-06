package org.example.rest;

import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.example.dto.ProductoDto;
import org.example.dto.ProductoPatchDto;
import org.example.lib.ProductoService;

import java.net.URI;

import static org.example.rest.ControllerRegistry.Endpoints.PRODUCTOS;

// El Controller SOLO traduce peticion HTTP -> llamada de metodo, y resultado de metodo ->
// respuesta HTTP (codigo de estado, headers). No sabe nada de negocio, no sabe nada de
// EJB/JPA -- el conflicto de FK al borrar se resuelve en el ExceptionMapper
// (EJBExceptionMapper), no aqui. Y no sabe nada de ControllerRegistry -- la relacion va en
// un solo sentido, el registro conoce a este Controller, este Controller no conoce al
// registro.
//
// Se registra en ControllerRegistry.register(ProductoController.class).
@Path(PRODUCTOS)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoController {

    @EJB
    private ProductoService productoService;

    // Para construir la URI del recurso creado en el header Location del POST.
    @Context
    private UriInfo uriInfo;

    @GET
    public Response listar() {
        return Response.ok(productoService.listar()).build();
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        ProductoDto dto = productoService.buscarPorId(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @POST
    public Response crear(@Valid ProductoDto dto) {
        ProductoDto creado = productoService.crear(dto);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(creado.id())).build();
        return Response.created(location).entity(creado).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") Long id, @Valid ProductoDto dto) {
        ProductoDto actualizado = productoService.actualizar(id, dto);
        if (actualizado == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(actualizado).build();
    }

    @PATCH
    @Path("/{id}")
    public Response patch(@PathParam("id") Long id, @Valid ProductoPatchDto cambios) {
        ProductoDto actualizado = productoService.patch(id, cambios);
        if (actualizado == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(actualizado).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        boolean eliminado = productoService.eliminar(id);
        if (!eliminado) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}

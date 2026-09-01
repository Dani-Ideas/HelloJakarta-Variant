package org.example.rest;

import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.example.dto.UsuarioDTO;
import org.example.lib.UsuarioService;

import java.net.URI;

// Generado por scripts/generar_capas.py siguiendo el mismo patron que ProductoResource --
// revisa el @Path (adivinanza simple, "usuarios") si el plural correcto es
// irregular (ej. SesionCaja -> "sesiones-caja").
@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @EJB
    private UsuarioService usuarioService;

    @Context
    private UriInfo uriInfo;

    @GET
    public Response listar() {
        return Response.ok(usuarioService.listar()).build();
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        UsuarioDTO dto = usuarioService.buscarPorId(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @POST
    public Response crear(@Valid UsuarioDTO dto) {
        UsuarioDTO creado = usuarioService.crear(dto);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(creado.id())).build();
        return Response.created(location).entity(creado).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") Long id, @Valid UsuarioDTO dto) {
        UsuarioDTO actualizado = usuarioService.actualizar(id, dto);
        if (actualizado == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(actualizado).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        boolean eliminado = usuarioService.eliminar(id);
        if (!eliminado) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}

package org.example.rest;

import jakarta.ejb.EJB;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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
import org.example.dto.FacturaDto;
import org.example.dto.FacturaPatchDto;
import org.example.lib.FacturaService;

import java.net.URI;

// A proposito NO tiene @DELETE: borrar una factura ya emitida no tiene sentido de
// negocio real (a diferencia de Producto, que si se puede dar de baja).
@Path("/facturas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FacturaController {

    @EJB
    private FacturaService facturaService;

    @Context
    private UriInfo uriInfo;

    @GET
    public Response listar() {
        return Response.ok(facturaService.listar()).build();
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        FacturaDto dto = facturaService.buscarPorId(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @POST
    public Response crear(@Valid FacturaDto dto) {
        FacturaDto creada = facturaService.crear(dto);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(creada.id())).build();
        return Response.created(location).entity(creada).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") Long id, @Valid FacturaDto dto) {
        FacturaDto actualizada = facturaService.actualizar(id, dto);
        if (actualizada == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(actualizada).build();
    }

    @PATCH
    @Path("/{id}")
    public Response patch(@PathParam("id") Long id, FacturaPatchDto cambios) {
        FacturaDto actualizada = facturaService.patch(id, cambios);
        if (actualizada == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(actualizada).build();
    }
}

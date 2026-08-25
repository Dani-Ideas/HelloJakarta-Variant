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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dto.FacturaDTO;
import org.example.dto.FacturaPatchDTO;
import org.example.lib.FacturaService;

import java.util.List;

// A proposito NO tiene @DELETE: borrar una factura ya emitida no tiene sentido de
// negocio real (a diferencia de Producto, que si se puede dar de baja).
@Path("/facturas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FacturaResource {

    @EJB
    private FacturaService facturaService;

    @GET
    public List<FacturaDTO> listar() {
        return facturaService.listar();
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        FacturaDTO dto = facturaService.buscarPorId(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @POST
    public Response crear(@Valid FacturaDTO dto) {
        FacturaDTO creada = facturaService.crear(dto);
        return Response.status(Response.Status.CREATED).entity(creada).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") Long id, @Valid FacturaDTO dto) {
        FacturaDTO actualizada = facturaService.actualizar(id, dto);
        if (actualizada == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(actualizada).build();
    }

    @PATCH
    @Path("/{id}")
    public Response patch(@PathParam("id") Long id, FacturaPatchDTO cambios) {
        FacturaDTO actualizada = facturaService.patch(id, cambios);
        if (actualizada == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(actualizada).build();
    }
}

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
import org.example.dto.FacturaDTO;
import org.example.lib.FacturaService;

import java.util.List;

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
}

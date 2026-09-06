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
import org.example.dto.SesionCajaDto;
import org.example.lib.SesionCajaService;

import java.util.List;

import static org.example.rest.ControllerRegistry.Endpoints.SESIONES_CAJA;

// Se registra en ControllerRegistry.register(SesionCajaController.class).
@Path(SESIONES_CAJA)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SesionCajaController {

    @EJB
    private SesionCajaService sesionCajaService;

    @GET
    public List<SesionCajaDto> listar() {
        return sesionCajaService.listar();
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        SesionCajaDto dto = sesionCajaService.buscarPorId(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @POST
    public Response crear(@Valid SesionCajaDto dto) {
        SesionCajaDto creada = sesionCajaService.crear(dto);
        return Response.status(Response.Status.CREATED).entity(creada).build();
    }
}

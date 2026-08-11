package org.example.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dto.FacturaDTO;
import org.example.mapper.FacturaMapper;
import org.example.model.Factura;
import org.example.service.FacturaService;

import java.util.List;
import java.util.stream.Collectors;

@Path("/facturas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FacturaResource {

    @EJB
    private FacturaService facturaService;

    @GET
    public List<FacturaDTO> listar() {
        return facturaService.listar().stream()
                .map(FacturaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        Factura factura = facturaService.buscarPorId(id);
        if (factura == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(FacturaMapper.toDTO(factura)).build();
    }

    @POST
    public Response crear(FacturaDTO dto) {
        Factura creada = facturaService.crear(FacturaMapper.toEntity(dto));
        return Response.status(Response.Status.CREATED).entity(FacturaMapper.toDTO(creada)).build();
    }
}

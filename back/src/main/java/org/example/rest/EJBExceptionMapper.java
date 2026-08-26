package org.example.rest;

import jakarta.ejb.EJBException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

// Reemplaza el enfoque anterior (RecursoEnUsoException + em.flush() forzado en el
// Service): en vez de forzar que el DELETE se ejecute de inmediato para poder atraparlo
// dentro de ProductoServiceImpl, se deja que la EJBException llegue de forma natural
// hasta este limite -- sin importar si el fallo ocurre dentro del metodo o al hacer commit
// DESPUES de que el metodo ya regreso (ambos casos terminan aqui igual). Con esto,
// ProductoServiceImpl.eliminar() vuelve a ser trivial: ni try/catch, ni EntityManager.
//
// Tambien cubre CUALQUIER otra EJBException no reconocida (de cualquier Resource, no solo
// Producto) con un mensaje generico -- antes, un fallo real de infraestructura hubiera
// escapado como un 500 crudo de GlassFish con el stack trace completo expuesto.
@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBException> {

    @Override
    public Response toResponse(EJBException exception) {
        if (esViolacionDeIntegridad(exception)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "No se puede completar la operacion: el recurso esta siendo usado en otro lado"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        // Cualquier otra causa (timeout, conexion caida, etc.) se responde generico --
        // nunca se expone el detalle interno real al cliente.
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Ocurrio un error interno"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private boolean esViolacionDeIntegridad(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
        }
        return false;
    }
}

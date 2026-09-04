package org.example.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

// El "controller de controllers": la puerta de entrada unica (@ApplicationPath("/api"))
// mas el registro EXPLICITO de que Controllers/Providers cuelgan de ella. Antes esto
// extendia Application sin overridear nada, y Jersey registraba cualquier @Path/@Provider
// que encontrara por classpath scanning. Con getClasses() overrideado, ese scanning
// automatico se apaga: si una clase no esta en el Set de abajo, NO se expone, aunque tenga
// @Path o @Provider -- hay que agregarla aqui a mano.
//
// La relacion va en un solo sentido: este registro conoce a cada Controller (los importa,
// los referencia en el Set), pero ningun Controller conoce a este registro -- por eso no
// hay ninguna anotacion ni import relacionado con esto en, por ejemplo, ProductoController.
//
// SpaFallbackFilter NO va aqui -- es un @WebFilter de Servlet (jakarta.servlet), no un
// @Provider de JAX-RS; el contenedor de Servlet lo registra por su cuenta, este Set solo
// controla lo que es JAX-RS.
@ApplicationPath("/api")
public class ControllerRegistry extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                // Controllers -- un endpoint por entidad
                ProductoController.class,
                FacturaController.class,
                SesionCajaController.class,
                UsuarioController.class,
                // Providers -- filtros/mappers transversales a todos los Controller
                CorsFilter.class,
                ValidationExceptionMapper.class,
                EJBExceptionMapper.class
        );
    }
}

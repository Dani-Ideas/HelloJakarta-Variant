package org.example.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

// Registro EXPLICITO de clases JAX-RS -- antes esto extendia Application sin overridear
// nada, y Jersey registraba cualquier @Path/@Provider que encontrara por classpath
// scanning. Con getClasses() overrideado, ese scanning automatico se apaga: si una clase
// no esta en el Set de abajo, NO se expone, aunque tenga @Path o @Provider -- hay que
// agregarla aqui a mano. Es el patron del proyecto real que se quiere imitar: un "registro
// central" que deja explicito que endpoints/filtros estan realmente activos, en vez de que
// dependa de que Jersey los encuentre solo.
//
// SpaFallbackFilter NO va aqui -- es un @WebFilter de Servlet (jakarta.servlet), no un
// @Provider de JAX-RS; el contenedor de Servlet lo registra por su cuenta, este Set solo
// controla lo que es JAX-RS.
@ApplicationPath("/api")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                // Resources -- un endpoint por entidad
                ProductoResource.class,
                FacturaResource.class,
                SesionCajaResource.class,
                UsuarioResource.class,
                // Providers -- filtros/mappers transversales a todos los Resource
                CorsFilter.class,
                ValidationExceptionMapper.class,
                EJBExceptionMapper.class
        );
    }
}

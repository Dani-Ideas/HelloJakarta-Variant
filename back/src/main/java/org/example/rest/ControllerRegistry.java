package org.example.rest;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

// El "controller de controllers": la puerta de entrada unica (@ApplicationPath("/api"))
// mas el registro EXPLICITO de que Controllers/Providers cuelgan de ella. Antes esto
// extendia Application sin overridear nada, y Jersey registraba cualquier @Path/@Provider
// que encontrara por classpath scanning. Ahora extiende ResourceConfig (una subclase de
// Application que trae Jersey, no algo estandar de JAX-RS puro) y cada clase se agrega a
// mano con register(...) dentro del constructor -- si una clase no pasa por register(),
// NO se expone, aunque tenga @Path o @Provider.
//
// La relacion va en un solo sentido: este registro conoce a cada Controller (los importa,
// los registra), pero ningun Controller conoce a este registro -- por eso no hay ninguna
// anotacion ni import relacionado con esto en, por ejemplo, ProductoController.
//
// SpaFallbackFilter NO va aqui -- es un @WebFilter de Servlet (jakarta.servlet), no un
// @Provider de JAX-RS; el contenedor de Servlet lo registra por su cuenta, esto solo
// controla lo que es JAX-RS.
@ApplicationPath(ControllerRegistry.Endpoints.API)
public class ControllerRegistry extends ResourceConfig {

    // Constantes de path -- una sola fuente de verdad para cada "/algo" que antes estaba
    // repetido como String literal dentro de cada @Path(...) de cada Controller. Cada
    // Controller las importa con "import static" (ver ProductoController, por ejemplo).
    //
    // Por que funciona: una anotacion solo acepta CONSTANTES DE COMPILACION como valor --
    // un "public static final String" cuenta como tal (el compilador la sustituye por su
    // valor literal al compilar), por eso @Path(Endpoints.PRODUCTOS) es legal exactamente
    // igual que @Path("/productos").
    //
    // Esto NO es lo que activa/desactiva un endpoint -- eso lo sigue haciendo el Set de
    // getClasses() de abajo (si una clase no esta en el Set, no se expone, tenga o no
    // @Path). Las constantes solo evitan que el mismo string quede tipeado 2 veces (aqui,
    // si algun dia se necesitara, y en el Controller) y se desincronicen por un typo.
    public static final class Endpoints {
        private Endpoints() {}

        public static final String API = "/api";
        public static final String PRODUCTOS = "/productos";
        public static final String FACTURAS = "/facturas";
        public static final String SESIONES_CAJA = "/sesiones-caja";
        public static final String USUARIOS = "/usuarios";
    }

    public ControllerRegistry() {
        // Controllers -- un endpoint por entidad
        register(ProductoController.class);
        register(FacturaController.class);
        register(SesionCajaController.class);
        register(UsuarioController.class);
        // Providers -- filtros/mappers transversales a todos los Controller
        register(CorsFilter.class);
        register(ValidationExceptionMapper.class);
        register(EJBExceptionMapper.class);
    }
}

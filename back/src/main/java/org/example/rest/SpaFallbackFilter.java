package org.example.rest;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URL;

// TanStack Router maneja rutas como /productos y /facturas del lado del navegador -- nunca
// existen como archivos reales en el WAR. Este filtro reemplaza al viejo <error-page> de
// web.xml (ese interceptaba TAMBIEN los 404 legitimos de la API, causando un 403 raro en
// PUT/DELETE -- ver bitacora-fixes.md, incidente 10).
//
// Regla: si es /api/* o si el archivo pedido existe de verdad (index.html, /assets/*.js,
// favicon.svg...), no se toca. Solo si NINGUNA de esas dos cosas aplica, se reenvia a
// index.html para que React + el router retomen el control y decidan que mostrar.
@WebFilter("/*")
public class SpaFallbackFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean esApi = path.startsWith("/api/") || path.equals("/api");
        URL archivoReal = request.getServletContext().getResource(path);

        if (esApi || archivoReal != null) {
            chain.doFilter(req, res);
            return;
        }

        request.getRequestDispatcher("/index.html").forward(req, res);
    }
}

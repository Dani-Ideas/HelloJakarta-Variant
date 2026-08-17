import { createRootRoute, createRoute, createRouter } from "@tanstack/react-router";
import { RootLayout } from "./routes/RootLayout";
import { HomePage } from "./routes/HomePage";
import { ProductosPage } from "./routes/ProductosPage";
import { FacturasPage } from "./routes/FacturasPage";

// Este archivo es el "mapa" completo de la app: que URL corresponde a que componente.
// No dibuja nada el mismo -- solo arma la estructura que despues usa <RouterProvider/>
// en main.tsx.

// La ruta raiz -- SIEMPRE se renderiza, sin importar la URL. Trae el <Outlet/>
// donde se inserta la pagina que corresponda.
const rootRoute = createRootRoute({
  component: RootLayout,
});

// Cada createRoute() es "hijo" de rootRoute, y dice: para esta URL, este componente.
// getParentRoute le dice al router donde "cuelga" esta ruta dentro del arbol (aqui,
// todas cuelgan directo de rootRoute, no hay sub-rutas anidadas todavia).
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/",
  component: HomePage,
});

const productosRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/productos",
  component: ProductosPage,
});

const facturasRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/facturas",
  component: FacturasPage,
});

// Junta las rutas sueltas en un solo arbol -- esto es lo que createRouter necesita para
// saber que existe en la app completa.
const routeTree = rootRoute.addChildren([indexRoute, productosRoute, facturasRoute]);

export const router = createRouter({
  routeTree,
  // El WAR se despliega bajo /HelloJakarta-variante/, no en la raiz del dominio -- mismo
  // problema que tuvimos con el "base" de Vite, misma solucion: decirle al router bajo
  // que ruta real vive, para que arme los links correctos. Sin esto, <Link to="/productos">
  // generaria un href de "/productos" a secas (raiz del dominio), en vez de
  // "/HelloJakarta-variante/productos" (donde realmente vive el WAR).
  basepath: "/HelloJakarta-variante",
});

// Registro de tipos: le da a TypeScript autocompletado y validacion de las rutas que
// existen (por eso <Link to="/productos"> se valida en tiempo de compilacion -- si
// escribieras <Link to="/no-existe">, TypeScript marcaria error antes de correr nada).
declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

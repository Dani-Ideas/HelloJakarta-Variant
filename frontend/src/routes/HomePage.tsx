import { Link } from "@tanstack/react-router";

// Pagina de la ruta "/" (ver indexRoute en router.tsx). Es la que se ve dentro del
// <Outlet/> de RootLayout cuando entras a la app por primera vez.
export function HomePage() {
  return (
    <section>
      <h2>Bienvenido</h2>
      <p>Elige qué quieres ver:</p>
      <div className="botones-inicio">
        {/* Mismo <Link> que en el nav -- aqui se ve mas como "boton" gracias a la clase
            CSS boton-nuevo, pero por dentro sigue siendo navegacion de router, no un
            <button> ni un <a> con recarga de pagina. */}
        <Link to="/productos" className="boton-nuevo">
          Ver productos
        </Link>
        <Link to="/facturas" className="boton-nuevo">
          Ver facturas
        </Link>
      </div>
    </section>
  );
}

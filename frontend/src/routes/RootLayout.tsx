import { Link, Outlet } from "@tanstack/react-router";

// Este componente es el "marco" fijo de toda la app: se renderiza SIEMPRE, sin importar
// en que pagina estes (/, /productos o /facturas). Es el component: de rootRoute en
// router.tsx -- por eso el header y el <nav> nunca "parpadean" al cambiar de pagina, solo
// el contenido de <Outlet/> cambia.
export function RootLayout() {
  return (
    <div className="pagina">
      <header className="encabezado">
        <h1>HelloJakarta</h1>
        <p className="subtitulo">Panel de práctica — Jakarta EE 10 + TanStack</p>

        <nav className="nav">
          {/* <Link> (de TanStack Router, NO un <a> normal) cambia de pagina sin recargar
              el navegador: intercepta el clic, actualiza la URL con el History API del
              navegador, y le dice al router que muestre otro componente en <Outlet/>. */}

          {/* activeOptions={{ exact: true }} es necesario SOLO en "/" -- sin esto, como
              toda URL empieza con "/", este link se marcaria "activo" tambien estando en
              /productos o /facturas. Los otros dos no lo necesitan porque "/productos"
              nunca es prefijo de otra ruta que exista. */}
          <Link to="/" activeOptions={{ exact: true }} activeProps={{ className: "nav-activo" }}>
            Inicio
          </Link>

          {/* activeProps le pone la clase CSS "nav-activo" automaticamente cuando la URL
              actual coincide con este link -- por eso se ve resaltado el que corresponde. */}
          <Link to="/productos" activeProps={{ className: "nav-activo" }}>
            Productos
          </Link>
          <Link to="/facturas" activeProps={{ className: "nav-activo" }}>
            Facturas
          </Link>
        </nav>
      </header>

      <main>
        {/* Aqui es donde el router "inserta" la pagina activa segun la URL actual:
            HomePage, ProductosPage o FacturasPage (ver router.tsx). */}
        <Outlet />
      </main>
    </div>
  );
}

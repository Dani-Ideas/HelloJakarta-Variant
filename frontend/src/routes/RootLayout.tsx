import { Outlet } from "@tanstack/react-router";

// Este componente es el "marco" fijo de toda la app: se renderiza SIEMPRE, sin importar
// en que pagina estes (/, /productos o /facturas). Es el component: de rootRoute en
// router.tsx -- por eso el header nunca "parpadea" al cambiar de pagina, solo el
// contenido de <Outlet/> cambia.
//
// Sin <nav> a proposito: la navegacion entre paginas vive en HomePage (la lista de
// botones + el PieMenu), no en un menu fijo arriba -- para volver a "/" desde cualquier
// pagina, usa el boton "atras" del navegador o escribe la URL.
export function RootLayout() {
  return (
    <div className="pagina">
      <header className="encabezado">
        <h1>HelloJakarta</h1>
        <p className="subtitulo">Panel de práctica — Jakarta EE 10 + TanStack</p>
      </header>

      <main>
        {/* Aqui es donde el router "inserta" la pagina activa segun la URL actual:
            HomePage, ProductosPage o FacturasPage (ver router.tsx). */}
        <Outlet />
      </main>
    </div>
  );
}

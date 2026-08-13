import { FacturasTable } from "./components/FacturasTable";
import { ProductosPanel } from "./components/ProductosPanel";

function App() {
  return (
    <div className="pagina">
      <header className="encabezado">
        <h1>HelloJakarta</h1>
        <p className="subtitulo">Panel de práctica — Jakarta EE 10 + TanStack</p>
      </header>

      <main>
        <section>
          <h2>Productos</h2>
          <ProductosPanel />
        </section>

        <section>
          <h2>Facturas</h2>
          <FacturasTable />
        </section>
      </main>
    </div>
  );
}

export default App;

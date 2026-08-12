import { FacturasTable } from "./components/FacturasTable";
import { ProductosTable } from "./components/ProductosTable";

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
          <ProductosTable />
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

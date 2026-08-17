import { FacturasTable } from "../components/FacturasTable";

// Pagina de la ruta "/facturas" (ver facturasRoute en router.tsx). Mismo patron que
// ProductosPage: solo envuelve el componente que ya existia (FacturasTable).
export function FacturasPage() {
  return (
    <section>
      <h2>Facturas</h2>
      <FacturasTable />
    </section>
  );
}

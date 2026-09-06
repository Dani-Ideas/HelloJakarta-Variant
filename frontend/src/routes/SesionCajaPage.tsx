import { SesionCajaPanel } from "../components/SesionCajaPanel";

// Pagina de la ruta "/sesiones-caja" -- mismo patron que ProductosPage/FacturasPage.
export function SesionCajaPage() {
  return (
    <section>
      <h2>Sesiones de caja</h2>
      <SesionCajaPanel />
    </section>
  );
}

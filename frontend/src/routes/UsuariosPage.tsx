import { UsuariosPanel } from "../components/UsuariosPanel";

// Pagina de la ruta "/usuarios" -- mismo patron que ProductosPage/FacturasPage.
export function UsuariosPage() {
  return (
    <section>
      <h2>Usuarios</h2>
      <UsuariosPanel />
    </section>
  );
}

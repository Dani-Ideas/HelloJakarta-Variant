import { ProductosPanel } from "../components/ProductosPanel";

// Pagina de la ruta "/productos" (ver productosRoute en router.tsx). No tiene logica
// propia -- es solo el "envoltorio de pagina" para el componente que ya existia antes de
// agregar routing (ProductosPanel, con su tabla + formulario de crear/editar/eliminar).
export function ProductosPage() {
  return (
    <section>
      <h2>Productos</h2>
      <ProductosPanel />
    </section>
  );
}

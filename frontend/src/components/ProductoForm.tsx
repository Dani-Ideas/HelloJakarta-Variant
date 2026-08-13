import { useState, type FormEvent } from "react";
import type { ProductoInput } from "../api/client";
import type { ProductoDTO } from "../api/types";

interface ProductoFormProps {
  productoInicial?: ProductoDTO;
  onGuardar: (datos: ProductoInput) => void;
  onCancelar: () => void;
  guardando: boolean;
}

export function ProductoForm({ productoInicial, onGuardar, onCancelar, guardando }: ProductoFormProps) {
  const [nombre, setNombre] = useState(productoInicial?.nombre ?? "");
  const [sku, setSku] = useState(productoInicial?.sku ?? "");
  const [precio, setPrecio] = useState(productoInicial?.precio.toString() ?? "");
  const [stock, setStock] = useState(productoInicial?.stock.toString() ?? "");

  function manejarEnvio(evento: FormEvent) {
    evento.preventDefault();
    onGuardar({
      nombre,
      sku,
      precio: Number(precio),
      stock: Number(stock),
    });
  }

  return (
    <form className="formulario" onSubmit={manejarEnvio}>
      <h3>{productoInicial ? `Editar: ${productoInicial.nombre}` : "Nuevo producto"}</h3>

      <div className="campo">
        <label htmlFor="nombre">Nombre</label>
        <input id="nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required />
      </div>

      <div className="campo">
        <label htmlFor="sku">SKU</label>
        <input id="sku" value={sku} onChange={(e) => setSku(e.target.value)} required />
      </div>

      <div className="campo">
        <label htmlFor="precio">Precio</label>
        <input
          id="precio"
          type="number"
          step="0.01"
          min="0.01"
          value={precio}
          onChange={(e) => setPrecio(e.target.value)}
          required
        />
      </div>

      <div className="campo">
        <label htmlFor="stock">Stock</label>
        <input
          id="stock"
          type="number"
          step="1"
          min="0"
          value={stock}
          onChange={(e) => setStock(e.target.value)}
          required
        />
      </div>

      <div className="acciones-formulario">
        <button type="submit" disabled={guardando}>
          {guardando ? "Guardando..." : "Guardar"}
        </button>
        <button type="button" className="boton-secundario" onClick={onCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

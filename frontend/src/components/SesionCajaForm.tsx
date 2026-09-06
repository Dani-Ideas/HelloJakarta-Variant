import { useState, type FormEvent } from "react";
import type { SesionCajaInput } from "../api/client";

interface SesionCajaFormProps {
  onGuardar: (datos: SesionCajaInput) => void;
  onCancelar: () => void;
  guardando: boolean;
}

// Solo "abrir caja" -- SesionCajaController no expone actualizar/eliminar, asi que este
// formulario nunca precarga datos de una sesion existente (a diferencia de ProductoForm,
// que sirve para crear Y editar).
export function SesionCajaForm({ onGuardar, onCancelar, guardando }: SesionCajaFormProps) {
  const [cajero, setCajero] = useState("");
  const [locacion, setLocacion] = useState("");
  const [montoApertura, setMontoApertura] = useState("");

  function manejarEnvio(evento: FormEvent) {
    evento.preventDefault();
    onGuardar({
      cajero,
      locacion,
      montoApertura: Number(montoApertura),
    });
  }

  return (
    <form className="formulario" onSubmit={manejarEnvio}>
      <h3>Abrir sesión de caja</h3>

      <div className="campo">
        <label htmlFor="cajero">Cajero</label>
        <input id="cajero" value={cajero} onChange={(e) => setCajero(e.target.value)} required />
      </div>

      <div className="campo">
        <label htmlFor="locacion">Ubicación</label>
        <input id="locacion" value={locacion} onChange={(e) => setLocacion(e.target.value)} required />
      </div>

      <div className="campo">
        <label htmlFor="montoApertura">Monto de apertura</label>
        <input
          id="montoApertura"
          type="number"
          step="0.01"
          min="0"
          value={montoApertura}
          onChange={(e) => setMontoApertura(e.target.value)}
          required
        />
      </div>

      <div className="acciones-formulario">
        <button type="submit" disabled={guardando}>
          {guardando ? "Abriendo..." : "Abrir caja"}
        </button>
        <button type="button" className="boton-secundario" onClick={onCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

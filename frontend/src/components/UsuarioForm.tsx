import { useState, type FormEvent } from "react";
import type { UsuarioInput } from "../api/client";
import type { Rol, UsuarioDTO } from "../api/types";

interface UsuarioFormProps {
  usuarioInicial?: UsuarioDTO;
  onGuardar: (datos: UsuarioInput) => void;
  onCancelar: () => void;
  guardando: boolean;
}

// Los 3 valores tienen que coincidir EXACTO con el enum Rol.java del backend -- si mandas
// un string que no sea uno de estos 3, Bean Validation lo rechaza con 400.
const ROLES: Rol[] = ["ADMIN", "VENDEDOR", "CAJERO"];

export function UsuarioForm({ usuarioInicial, onGuardar, onCancelar, guardando }: UsuarioFormProps) {
  const [nombre, setNombre] = useState(usuarioInicial?.nombre ?? "");
  const [rol, setRol] = useState<Rol>(usuarioInicial?.rol ?? "CAJERO");

  function manejarEnvio(evento: FormEvent) {
    evento.preventDefault();
    onGuardar({ nombre, rol });
  }

  return (
    <form className="formulario" onSubmit={manejarEnvio}>
      <h3>{usuarioInicial ? `Editar: ${usuarioInicial.nombre}` : "Nuevo usuario"}</h3>

      <div className="campo">
        <label htmlFor="nombre">Nombre</label>
        <input id="nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required />
      </div>

      <div className="campo">
        <label htmlFor="rol">Rol</label>
        <select id="rol" value={rol} onChange={(e) => setRol(e.target.value as Rol)}>
          {ROLES.map((valor) => (
            <option key={valor} value={valor}>
              {valor}
            </option>
          ))}
        </select>
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

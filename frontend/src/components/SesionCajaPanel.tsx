import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { crearSesionCaja, fetchSesionesCaja } from "../api/client";
import type { SesionCajaInput } from "../api/client";
import { SesionCajaForm } from "./SesionCajaForm";
import { SesionCajaTable } from "./SesionCajaTable";

// Mismo patron que ProductosPanel, mas simple: sin "editar" ni "eliminar" porque el
// backend (SesionCajaController) no los expone.
export function SesionCajaPanel() {
  const queryClient = useQueryClient();
  const [mostrarFormulario, setMostrarFormulario] = useState(false);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["sesiones-caja"],
    queryFn: fetchSesionesCaja,
  });

  const crearMutation = useMutation({
    mutationFn: crearSesionCaja,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sesiones-caja"] });
      setMostrarFormulario(false);
    },
  });

  function manejarGuardar(datos: SesionCajaInput) {
    crearMutation.mutate(datos);
  }

  if (isLoading) return <p className="estado">Cargando sesiones de caja...</p>;
  if (isError) return <p className="estado estado-error">Error: {(error as Error).message}</p>;

  return (
    <div>
      {mostrarFormulario ? (
        <SesionCajaForm
          onGuardar={manejarGuardar}
          onCancelar={() => setMostrarFormulario(false)}
          guardando={crearMutation.isPending}
        />
      ) : (
        <button className="boton-nuevo" onClick={() => setMostrarFormulario(true)}>
          + Abrir caja
        </button>
      )}

      {crearMutation.error && (
        <p className="estado estado-error">{(crearMutation.error as Error).message}</p>
      )}

      <SesionCajaTable sesiones={data ?? []} />
    </div>
  );
}

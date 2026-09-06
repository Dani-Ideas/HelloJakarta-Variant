import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { createUsuario, deleteUsuario, fetchUsuarios, updateUsuario } from "../api/client";
import type { UsuarioInput } from "../api/client";
import type { UsuarioDTO } from "../api/types";
import { UsuarioForm } from "./UsuarioForm";
import { UsuariosTable } from "./UsuariosTable";

// Mismo patron exacto que ProductosPanel -- UsuarioController expone los mismos 4 verbos
// (crear/actualizar/eliminar + listar) que ProductoController.
export function UsuariosPanel() {
  const queryClient = useQueryClient();
  const [editando, setEditando] = useState<UsuarioDTO | null>(null);
  const [mostrarFormulario, setMostrarFormulario] = useState(false);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["usuarios"],
    queryFn: fetchUsuarios,
  });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ["usuarios"] });

  const crearMutation = useMutation({
    mutationFn: createUsuario,
    onSuccess: () => {
      invalidar();
      setMostrarFormulario(false);
    },
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, datos }: { id: number; datos: UsuarioInput }) => updateUsuario(id, datos),
    onSuccess: () => {
      invalidar();
      setEditando(null);
    },
  });

  const eliminarMutation = useMutation({
    mutationFn: (usuario: UsuarioDTO) => deleteUsuario(usuario.id),
    onSuccess: () => invalidar(),
  });

  function manejarGuardar(datos: UsuarioInput) {
    if (editando) {
      actualizarMutation.mutate({ id: editando.id, datos });
    } else {
      crearMutation.mutate(datos);
    }
  }

  function manejarEliminar(usuario: UsuarioDTO) {
    if (window.confirm(`Eliminar "${usuario.nombre}"?`)) {
      eliminarMutation.mutate(usuario);
    }
  }

  function cerrarFormulario() {
    setMostrarFormulario(false);
    setEditando(null);
  }

  const formularioAbierto = mostrarFormulario || editando !== null;
  const guardando = crearMutation.isPending || actualizarMutation.isPending;
  const errorMutacion = crearMutation.error ?? actualizarMutation.error ?? eliminarMutation.error;

  if (isLoading) return <p className="estado">Cargando usuarios...</p>;
  if (isError) return <p className="estado estado-error">Error: {(error as Error).message}</p>;

  return (
    <div>
      {formularioAbierto ? (
        <UsuarioForm
          key={editando?.id ?? "nuevo"}
          usuarioInicial={editando ?? undefined}
          onGuardar={manejarGuardar}
          onCancelar={cerrarFormulario}
          guardando={guardando}
        />
      ) : (
        <button className="boton-nuevo" onClick={() => setMostrarFormulario(true)}>
          + Nuevo usuario
        </button>
      )}

      {errorMutacion && <p className="estado estado-error">{(errorMutacion as Error).message}</p>}

      <UsuariosTable
        usuarios={data ?? []}
        onEditar={(usuario) => {
          setEditando(usuario);
          setMostrarFormulario(false);
        }}
        onEliminar={manejarEliminar}
      />
    </div>
  );
}

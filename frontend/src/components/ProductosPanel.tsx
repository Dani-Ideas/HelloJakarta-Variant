import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { createProducto, deleteProducto, fetchProductos, updateProducto } from "../api/client";
import type { ProductoInput } from "../api/client";
import type { ProductoDTO } from "../api/types";
import { ProductoForm } from "./ProductoForm";
import { ProductosTable } from "./ProductosTable";

export function ProductosPanel() {
  const queryClient = useQueryClient();
  const [editando, setEditando] = useState<ProductoDTO | null>(null);
  const [mostrarFormulario, setMostrarFormulario] = useState(false);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["productos"],
    queryFn: fetchProductos,
  });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ["productos"] });

  const crearMutation = useMutation({
    mutationFn: createProducto,
    onSuccess: () => {
      invalidar();
      setMostrarFormulario(false);
    },
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, datos }: { id: number; datos: ProductoInput }) => updateProducto(id, datos),
    onSuccess: () => {
      invalidar();
      setEditando(null);
    },
  });

  const eliminarMutation = useMutation({
    mutationFn: (producto: ProductoDTO) => deleteProducto(producto.id),
    onSuccess: () => invalidar(),
  });

  function manejarGuardar(datos: ProductoInput) {
    if (editando) {
      actualizarMutation.mutate({ id: editando.id, datos });
    } else {
      crearMutation.mutate(datos);
    }
  }

  function manejarEliminar(producto: ProductoDTO) {
    if (window.confirm(`Eliminar "${producto.nombre}"?`)) {
      eliminarMutation.mutate(producto);
    }
  }

  function cerrarFormulario() {
    setMostrarFormulario(false);
    setEditando(null);
  }

  const formularioAbierto = mostrarFormulario || editando !== null;
  const guardando = crearMutation.isPending || actualizarMutation.isPending;
  const errorMutacion = crearMutation.error ?? actualizarMutation.error ?? eliminarMutation.error;

  if (isLoading) return <p className="estado">Cargando productos...</p>;
  if (isError) return <p className="estado estado-error">Error: {(error as Error).message}</p>;

  return (
    <div>
      {formularioAbierto ? (
        <ProductoForm
          key={editando?.id ?? "nuevo"}
          productoInicial={editando ?? undefined}
          onGuardar={manejarGuardar}
          onCancelar={cerrarFormulario}
          guardando={guardando}
        />
      ) : (
        <button className="boton-nuevo" onClick={() => setMostrarFormulario(true)}>
          + Nuevo producto
        </button>
      )}

      {errorMutacion && <p className="estado estado-error">{(errorMutacion as Error).message}</p>}

      <ProductosTable
        productos={data ?? []}
        onEditar={(producto) => {
          setEditando(producto);
          setMostrarFormulario(false);
        }}
        onEliminar={manejarEliminar}
      />
    </div>
  );
}

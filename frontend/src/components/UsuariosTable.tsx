import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table";
import type { UsuarioDTO } from "../api/types";

const columnHelper = createColumnHelper<UsuarioDTO>();

interface UsuariosTableProps {
  usuarios: UsuarioDTO[];
  onEditar: (usuario: UsuarioDTO) => void;
  onEliminar: (usuario: UsuarioDTO) => void;
}

export function UsuariosTable({ usuarios, onEditar, onEliminar }: UsuariosTableProps) {
  const columns = [
    columnHelper.accessor("nombre", { header: "Nombre" }),
    columnHelper.accessor("rol", { header: "Rol" }),
    columnHelper.display({
      id: "acciones",
      header: "",
      cell: ({ row }) => (
        <div className="acciones-fila">
          <button className="boton-link" onClick={() => onEditar(row.original)}>
            Editar
          </button>
          <button className="boton-link boton-peligro" onClick={() => onEliminar(row.original)}>
            Eliminar
          </button>
        </div>
      ),
    }),
  ];

  const table = useReactTable({
    data: usuarios,
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  return (
    <table className="tabla">
      <thead>
        {table.getHeaderGroups().map((headerGroup) => (
          <tr key={headerGroup.id}>
            {headerGroup.headers.map((header) => (
              <th key={header.id}>{flexRender(header.column.columnDef.header, header.getContext())}</th>
            ))}
          </tr>
        ))}
      </thead>
      <tbody>
        {table.getRowModel().rows.map((row) => (
          <tr key={row.id}>
            {row.getVisibleCells().map((cell) => (
              <td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

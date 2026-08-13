import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table";
import type { ProductoDTO } from "../api/types";

const formatoMoneda = new Intl.NumberFormat("es-MX", {
  style: "currency",
  currency: "USD",
});

const columnHelper = createColumnHelper<ProductoDTO>();

interface ProductosTableProps {
  productos: ProductoDTO[];
  onEditar: (producto: ProductoDTO) => void;
  onEliminar: (producto: ProductoDTO) => void;
}

export function ProductosTable({ productos, onEditar, onEliminar }: ProductosTableProps) {
  const columns = [
    columnHelper.accessor("nombre", { header: "Nombre" }),
    columnHelper.accessor("sku", { header: "SKU" }),
    columnHelper.accessor("precio", {
      header: "Precio",
      cell: (info) => formatoMoneda.format(info.getValue()),
      meta: { align: "right" },
    }),
    columnHelper.accessor("stock", {
      header: "Stock",
      meta: { align: "right" },
    }),
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
    data: productos,
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  return (
    <table className="tabla">
      <thead>
        {table.getHeaderGroups().map((headerGroup) => (
          <tr key={headerGroup.id}>
            {headerGroup.headers.map((header) => (
              <th
                key={header.id}
                className={header.column.columnDef.meta?.align === "right" ? "num" : undefined}
              >
                {flexRender(header.column.columnDef.header, header.getContext())}
              </th>
            ))}
          </tr>
        ))}
      </thead>
      <tbody>
        {table.getRowModel().rows.map((row) => (
          <tr key={row.id}>
            {row.getVisibleCells().map((cell) => (
              <td
                key={cell.id}
                className={cell.column.columnDef.meta?.align === "right" ? "num" : undefined}
              >
                {flexRender(cell.column.columnDef.cell, cell.getContext())}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

import { useQuery } from "@tanstack/react-query";
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table";
import { fetchProductos } from "../api/client";
import type { ProductoDTO } from "../api/types";

const formatoMoneda = new Intl.NumberFormat("es-MX", {
  style: "currency",
  currency: "USD",
});

const columnHelper = createColumnHelper<ProductoDTO>();

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
];

export function ProductosTable() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["productos"],
    queryFn: fetchProductos,
  });

  const table = useReactTable({
    data: data ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  if (isLoading) return <p className="estado">Cargando productos...</p>;
  if (isError) return <p className="estado estado-error">Error: {(error as Error).message}</p>;

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

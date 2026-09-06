import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table";
import type { SesionCajaDTO } from "../api/types";

const formatoMoneda = new Intl.NumberFormat("es-MX", {
  style: "currency",
  currency: "USD",
});

const formatoFecha = new Intl.DateTimeFormat("es-MX", {
  dateStyle: "short",
  timeStyle: "short",
});

function formatearFecha(valor: string | null): string {
  return valor ? formatoFecha.format(new Date(valor)) : "—";
}

const columnHelper = createColumnHelper<SesionCajaDTO>();

const columns = [
  columnHelper.accessor("cajero", { header: "Cajero" }),
  columnHelper.accessor("locacion", { header: "Ubicación" }),
  columnHelper.accessor("fApertura", {
    header: "Apertura",
    cell: (info) => formatearFecha(info.getValue()),
  }),
  columnHelper.accessor("fCierre", {
    header: "Cierre",
    cell: (info) => formatearFecha(info.getValue()),
  }),
  columnHelper.accessor("montoApertura", {
    header: "Monto apertura",
    cell: (info) => formatoMoneda.format(info.getValue()),
    meta: { align: "right" },
  }),
  columnHelper.accessor("cerrada", {
    header: "Estado",
    cell: (info) => (info.getValue() ? "Cerrada" : "Abierta"),
  }),
];

interface SesionCajaTableProps {
  sesiones: SesionCajaDTO[];
}

export function SesionCajaTable({ sesiones }: SesionCajaTableProps) {
  const table = useReactTable({
    data: sesiones,
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

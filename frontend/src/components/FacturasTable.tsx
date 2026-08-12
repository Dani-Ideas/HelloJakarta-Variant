import { useQuery } from "@tanstack/react-query";
import {
  type ExpandedState,
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  getExpandedRowModel,
  useReactTable,
} from "@tanstack/react-table";
import { Fragment, useState } from "react";
import { fetchFacturas } from "../api/client";
import type { FacturaDTO } from "../api/types";

const formatoMoneda = new Intl.NumberFormat("es-MX", {
  style: "currency",
  currency: "USD",
});

const columnHelper = createColumnHelper<FacturaDTO>();

const columns = [
  columnHelper.display({
    id: "expander",
    header: () => null,
    cell: ({ row }) => (
      <button
        className="expander"
        onClick={row.getToggleExpandedHandler()}
        aria-label={row.getIsExpanded() ? "Colapsar" : "Expandir"}
      >
        {row.getIsExpanded() ? "−" : "+"}
      </button>
    ),
  }),
  columnHelper.accessor("numero", { header: "Numero" }),
  columnHelper.accessor("cliente", { header: "Cliente" }),
  columnHelper.accessor("fecha", { header: "Fecha" }),
  columnHelper.accessor("total", {
    header: "Total",
    cell: (info) => formatoMoneda.format(info.getValue()),
    meta: { align: "right" },
  }),
];

export function FacturasTable() {
  const [expanded, setExpanded] = useState<ExpandedState>({});

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["facturas"],
    queryFn: fetchFacturas,
  });

  const table = useReactTable({
    data: data ?? [],
    columns,
    state: { expanded },
    onExpandedChange: setExpanded,
    getRowCanExpand: () => true,
    getCoreRowModel: getCoreRowModel(),
    getExpandedRowModel: getExpandedRowModel(),
  });

  if (isLoading) return <p className="estado">Cargando facturas...</p>;
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
          <Fragment key={row.id}>
            <tr>
              {row.getVisibleCells().map((cell) => (
                <td
                  key={cell.id}
                  className={cell.column.columnDef.meta?.align === "right" ? "num" : undefined}
                >
                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </td>
              ))}
            </tr>
            {row.getIsExpanded() && (
              <tr>
                <td colSpan={columns.length} className="detalle-celda">
                  <table className="tabla tabla-anidada">
                    <thead>
                      <tr>
                        <th>Producto</th>
                        <th className="num">Cantidad</th>
                        <th className="num">Precio unitario</th>
                        <th className="num">Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      {row.original.detalles.map((detalle) => (
                        <tr key={detalle.id}>
                          <td>{detalle.nombreProducto}</td>
                          <td className="num">{detalle.cantidad}</td>
                          <td className="num">{formatoMoneda.format(detalle.precioUnitario)}</td>
                          <td className="num">{formatoMoneda.format(detalle.subtotal)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </td>
              </tr>
            )}
          </Fragment>
        ))}
      </tbody>
    </table>
  );
}

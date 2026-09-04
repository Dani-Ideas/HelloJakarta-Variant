export interface ProductoDTO {
  id: number;
  nombre: string;
  sku: string;
  precio: number;
  stock: number;
}

export interface FacturaDetalleDTO {
  id: number;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  // Objeto anidado, no productoId/nombreProducto sueltos -- FacturaDetalleDto (backend)
  // cambio de forma en Documentation/bitacora-fixes.md incidente #18.
  producto: ProductoDTO;
}

export interface FacturaDTO {
  id: number;
  numero: string;
  fecha: string;
  cliente: string;
  total: number;
  detalles: FacturaDetalleDTO[];
}

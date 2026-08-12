export interface ProductoDTO {
  id: number;
  nombre: string;
  sku: string;
  precio: number;
  stock: number;
}

export interface FacturaDetalleDTO {
  id: number;
  productoId: number;
  nombreProducto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface FacturaDTO {
  id: number;
  numero: string;
  fecha: string;
  cliente: string;
  total: number;
  detalles: FacturaDetalleDTO[];
}

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

export interface SesionCajaDTO {
  id: number;
  cajero: string;
  cerrada: boolean;
  fApertura: string | null;
  fCierre: string | null;
  locacion: string;
  montoApertura: number;
  montoCierre: number | null;
}

// Mismos 3 valores que el enum Rol.java del backend -- si el backend agrega un valor
// nuevo, hay que agregarlo aqui tambien a mano (TypeScript no lo sabe solo).
export type Rol = "ADMIN" | "VENDEDOR" | "CAJERO";

export interface UsuarioDTO {
  id: number;
  nombre: string;
  rol: Rol;
}

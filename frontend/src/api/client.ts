import type { FacturaDTO, ProductoDTO, SesionCajaDTO, UsuarioDTO } from "./types";

// Ruta relativa: en `npm run dev` la resuelve el proxy de vite.config.ts hacia GlassFish;
// en produccion, el frontend vive dentro del mismo WAR, asi que resuelve al mismo origen.
const API_BASE = "/HelloJakarta-variante/api";

async function parseErrorBody(response: Response): Promise<string> {
  try {
    const body = await response.json();
    if (typeof body === "object" && body !== null) {
      return Object.values(body).join(", ");
    }
  } catch {
    // el body no era JSON valido, se usa el mensaje generico de abajo
  }
  return `${response.status} ${response.statusText}`;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!response.ok) {
    throw new Error(await parseErrorBody(response));
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

export function fetchProductos(): Promise<ProductoDTO[]> {
  return request<ProductoDTO[]>("/productos");
}

export function fetchFacturas(): Promise<FacturaDTO[]> {
  return request<FacturaDTO[]>("/facturas");
}

export type ProductoInput = Omit<ProductoDTO, "id">;

export function createProducto(producto: ProductoInput): Promise<ProductoDTO> {
  return request<ProductoDTO>("/productos", {
    method: "POST",
    body: JSON.stringify(producto),
  });
}

export function updateProducto(id: number, producto: ProductoInput): Promise<ProductoDTO> {
  return request<ProductoDTO>(`/productos/${id}`, {
    method: "PUT",
    body: JSON.stringify(producto),
  });
}

export function deleteProducto(id: number): Promise<void> {
  return request<void>(`/productos/${id}`, { method: "DELETE" });
}

// SesionCajaController solo expone listar/buscar/crear -- sin actualizar/eliminar por
// REST (ver Documentation/bitacora-fixes.md), por eso aqui solo hay 2 funciones, no 5.
export function fetchSesionesCaja(): Promise<SesionCajaDTO[]> {
  return request<SesionCajaDTO[]>("/sesiones-caja");
}

export type SesionCajaInput = Pick<SesionCajaDTO, "cajero" | "locacion" | "montoApertura">;

export function crearSesionCaja(sesion: SesionCajaInput): Promise<SesionCajaDTO> {
  return request<SesionCajaDTO>("/sesiones-caja", {
    method: "POST",
    body: JSON.stringify(sesion),
  });
}

export function fetchUsuarios(): Promise<UsuarioDTO[]> {
  return request<UsuarioDTO[]>("/usuarios");
}

export type UsuarioInput = Omit<UsuarioDTO, "id">;

export function createUsuario(usuario: UsuarioInput): Promise<UsuarioDTO> {
  return request<UsuarioDTO>("/usuarios", {
    method: "POST",
    body: JSON.stringify(usuario),
  });
}

export function updateUsuario(id: number, usuario: UsuarioInput): Promise<UsuarioDTO> {
  return request<UsuarioDTO>(`/usuarios/${id}`, {
    method: "PUT",
    body: JSON.stringify(usuario),
  });
}

export function deleteUsuario(id: number): Promise<void> {
  return request<void>(`/usuarios/${id}`, { method: "DELETE" });
}

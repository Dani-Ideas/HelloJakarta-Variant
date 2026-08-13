import type { FacturaDTO, ProductoDTO } from "./types";

const API_BASE = "http://localhost:8080/HelloJakarta/api";

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

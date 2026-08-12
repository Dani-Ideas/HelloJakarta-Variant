import type { FacturaDTO, ProductoDTO } from "./types";

const API_BASE = "http://localhost:8080/HelloJakarta/api";

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`);
  if (!response.ok) {
    throw new Error(`${path} respondio ${response.status}`);
  }
  return response.json();
}

export function fetchProductos(): Promise<ProductoDTO[]> {
  return getJson<ProductoDTO[]>("/productos");
}

export function fetchFacturas(): Promise<FacturaDTO[]> {
  return getJson<FacturaDTO[]>("/facturas");
}

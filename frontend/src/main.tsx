import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { router } from "./router";
import "./index.css";

const queryClient = new QueryClient();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    {/* QueryClientProvider sigue envolviendo todo, igual que antes de agregar routing --
        asi cualquier pagina (Home/Productos/Facturas) puede usar useQuery. */}
    <QueryClientProvider client={queryClient}>
      {/* Antes aqui iba <App/> directo. Ahora <RouterProvider> es el que decide que
          mostrar, leyendo la URL actual y consultando el "mapa" armado en router.tsx. */}
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);

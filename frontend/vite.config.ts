import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // El WAR se despliega bajo /HelloJakarta-variante/, no en la raiz del dominio -- sin
  // esto, los <script>/<link> del index.html generado apuntarian a rutas absolutas
  // equivocadas (la raiz del servidor en vez del context root del WAR).
  base: "/HelloJakarta-variante/",
  build: {
    // El build de produccion cae directo dentro del WAR de Jakarta EE, para que
    // GlassFish lo sirva como parte del mismo artefacto desplegado (monolito).
    outDir: "../back/src/main/webapp",
    emptyOutDir: true,
  },
  server: {
    // Solo aplica en `npm run dev` (localhost:5173) -- reenvia las llamadas a la API
    // hacia GlassFish sin que el navegador vea un origen distinto, asi no hace falta CORS.
    proxy: {
      "/HelloJakarta-variante/api": "http://localhost:8080",
    },
  },
});

import { ClipboardListIcon, LogOutIcon, WalletIcon } from "lucide-react"
import { PieMenu } from "../components/PieMenu"

// Pagina de la ruta "/" (ver indexRoute en router.tsx). Es la que se ve dentro del
// <Outlet/> de RootLayout cuando entras a la app por primera vez.
export function HomePage() {
  return (
    <>
      <section>
        <PieMenu
          items={[
            {
              to: "/salir-sitio",
              label: "Opción 1",
              descripcion: "Te avisa antes de salir del sitio y abre la consola de GlassFish en una pestaña nueva.",
              icon: LogOutIcon,
            },
            {
              to: "/formulario-pago",
              label: "Opción 2",
              descripcion: "Carrusel de pago que avanza solo y crea un producto real en la base de datos al simular el pago.",
              icon: WalletIcon,
            },
            {
              to: "/formulario-largo",
              label: "Opción 3",
              descripcion: "Formulario tradicional y largo (sin carrusel), 100% visual.",
              icon: ClipboardListIcon,
            },
          ]}
          size={280}
        />
      </section>
    </>
  )
}

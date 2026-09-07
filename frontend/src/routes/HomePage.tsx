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
            { to: "/salir-sitio", label: "Opción 1", icon: LogOutIcon },
            { to: "/formulario-pago", label: "Opción 2", icon: WalletIcon },
            { to: "/formulario-largo", label: "Opción 3", icon: ClipboardListIcon },
          ]}
          size={280}
        />
      </section>
    </>
  )
}

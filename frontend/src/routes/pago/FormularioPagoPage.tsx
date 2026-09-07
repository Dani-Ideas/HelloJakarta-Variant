import { useEffect, useRef, useState } from "react"
import { useMutation } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
import { cn } from "@/lib/utils"
import { createProducto } from "@/api/client"
import type { ProductoDTO } from "@/api/types"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter } from "@/components/ui/card"
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  type CarouselApi,
} from "@/components/ui/carousel"
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuList,
  navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu"
import { Checkbox } from "@/components/ui/checkbox"
import { NativeSelect } from "@/components/ui/native-select"
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Switch } from "@/components/ui/switch"
import { Spinner } from "@/components/ui/spinner"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  Attachment,
  AttachmentContent,
  AttachmentDescription,
  AttachmentMedia,
  AttachmentTitle,
} from "@/components/ui/attachment"
import { ArrowLeftIcon, CheckCircle2Icon, FileTextIcon, TriangleAlertIcon } from "lucide-react"

type Preferencia = "opcion1" | "opcion2" | "opcion3"
type EstadoPago = "idle" | "procesando" | "listo"

const PASOS = ["Preferencia", "Categoría", "Producto", "Pago", "Resumen"] as const
const ULTIMO_PASO = PASOS.length - 1
// Paso 0 SIEMPRE espera este tiempo (da chance de corregir antes de avanzar). Del paso 1
// en adelante se usa el rapido, salvo que el usuario ya haya usado "Atras"/el menu de
// pasos para regresar alguna vez -- desde ese momento se vuelve al lento para todo lo que
// falta (ver regresoAlgunaVezRef).
const RETRASO_CUIDADOSO_MS = 2000
const RETRASO_RAPIDO_MS = 1000

const OPCIONES_PREFERENCIA: { valor: Preferencia; titulo: string; descripcion: string }[] = [
  { valor: "opcion1", titulo: "Opción 1", descripcion: "Plan básico, facturación simple." },
  { valor: "opcion2", titulo: "Opción 2", descripcion: "Plan intermedio, con reportes." },
  { valor: "opcion3", titulo: "Opción 3", descripcion: "Plan completo, todo incluido." },
]

// Formulario de pago simulado: un carrusel de shadcn que avanza SOLO en cuanto el paso
// actual queda valido (no hace falta tocar "Siguiente" -- ver el useEffect de
// autoavance mas abajo). El pago en si sigue siendo 100% falso, pero al "pagar" si se
// crea un producto real via POST /productos (misma API/estructura que usa la pagina de
// Productos), para simular que la pasarela le avisa a la base de datos que la compra se
// completo.
export function FormularioPagoPage() {
  const [preferencia, setPreferencia] = useState<Preferencia | null>(null)
  const [categoria, setCategoria] = useState("")
  const [nombreProducto, setNombreProducto] = useState("")
  const [sku, setSku] = useState("")
  const [precio, setPrecio] = useState("")
  const [stock, setStock] = useState("")
  const [pagoActivado, setPagoActivado] = useState(false)
  const [pagoEstado, setPagoEstado] = useState<EstadoPago>("idle")
  const [productoCreado, setProductoCreado] = useState<ProductoDTO | null>(null)
  const [enviado, setEnviado] = useState(false)

  const [api, setApi] = useState<CarouselApi>()
  const [current, setCurrent] = useState(0)
  const [maxStep, setMaxStep] = useState(0)
  const [aviso, setAviso] = useState<string | null>(null)

  // Espejo en ref de current/maxStep: el listener "select" de Embla (mas abajo) puede
  // dispararse en el mismo tick en el que llamamos api.scrollTo(), antes de que React
  // vuelva a renderizar con el nuevo estado -- si ese listener leyera maxStep/current del
  // closure (el valor de la render anterior), se confundiria y regresaria el carrusel al
  // paso viejo justo despues de desbloquear el nuevo (asi se veia el bug: "se desbloquea
  // pero no avanza"). Con refs, siempre lee el valor mas reciente sin esperar al re-render.
  const maxStepRef = useRef(0)
  const currentRef = useRef(0)
  // true en cuanto el usuario usa "Atras" o el menu de pasos para ir a una etapa anterior
  // -- una vez true, ya no se vuelve a false en este montaje del componente (se resetea
  // solo si sales de la pagina y vuelves a entrar).
  const regresoAlgunaVezRef = useRef(false)

  function desbloquearHasta(destino: number) {
    maxStepRef.current = Math.max(maxStepRef.current, destino)
    setMaxStep(maxStepRef.current)
  }

  function retrasoAutoavance(paso: number): number {
    return paso === 0 || regresoAlgunaVezRef.current ? RETRASO_CUIDADOSO_MS : RETRASO_RAPIDO_MS
  }

  // Mismo endpoint/estructura que ProductosPanel.tsx (createProducto de api/client.ts) --
  // aqui no hay tabla ni edicion, solo un POST cuando el usuario "paga".
  const crearProductoMutation = useMutation({ mutationFn: createProducto })

  function datosValidosProducto(): boolean {
    const precioNum = Number(precio)
    const stockNum = Number(stock)
    return (
      nombreProducto.trim().length > 0 &&
      sku.trim().length > 0 &&
      precio.trim().length > 0 &&
      !Number.isNaN(precioNum) &&
      precioNum > 0 &&
      stock.trim().length > 0 &&
      !Number.isNaN(stockNum) &&
      stockNum >= 0 &&
      Number.isInteger(stockNum)
    )
  }

  function pasoValido(indice: number): boolean {
    switch (indice) {
      case 0:
        return preferencia !== null
      case 1:
        return categoria !== ""
      case 2:
        return datosValidosProducto()
      case 3:
        return pagoEstado === "listo"
      default:
        return true
    }
  }

  // El carrusel se puede mover por swipe/teclado por fuera de nuestros botones --
  // aqui lo "regresamos" si alguien llega (por el motivo que sea) a un paso todavia
  // no desbloqueado, para que la validacion no se pueda saltar. Lee/escribe siempre los
  // refs (nunca el state cerrado en el closure) para no pisarse con el auto-avance.
  useEffect(() => {
    if (!api) return
    function onSelect() {
      const indice = api!.selectedScrollSnap()
      if (indice > maxStepRef.current) {
        api!.scrollTo(maxStepRef.current)
        return
      }
      if (indice < currentRef.current) {
        regresoAlgunaVezRef.current = true
      }
      currentRef.current = indice
      setCurrent(indice)
    }
    onSelect()
    api.on("select", onSelect)
    api.on("reInit", onSelect)
    return () => {
      api.off("select", onSelect)
      api.off("reInit", onSelect)
    }
  }, [api])

  // Autoavance: si el paso EN EL QUE VAS (current === maxStep, o sea el mas nuevo, no uno
  // que ya pasaste y estas repasando con "Atras") queda valido y se mantiene valido sin
  // que nada cambie por retrasoAutoavance(current) ms, se pasa solo al siguiente -- sin
  // que haga falta tocar "Siguiente". El effect se re-dispara con cada tecleo (por las
  // deps de abajo) y limpia su propio setTimeout, asi que en la practica es un debounce:
  // solo avanza cuando el usuario deja de escribir y el paso ya quedo bien lleno.
  useEffect(() => {
    if (current !== maxStep) return
    if (current === ULTIMO_PASO) return
    if (!pasoValido(current)) return

    const destino = current + 1
    const timer = window.setTimeout(() => {
      setAviso(null)
      desbloquearHasta(destino)
      api?.scrollTo(destino)
    }, retrasoAutoavance(current))

    return () => window.clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current, maxStep, preferencia, categoria, nombreProducto, sku, precio, stock, pagoEstado])

  function irAPaso(indice: number) {
    if (indice > maxStepRef.current) return
    setAviso(null)
    api?.scrollTo(indice)
  }

  function anterior() {
    setAviso(null)
    api?.scrollTo(Math.max(current - 1, 0))
  }

  function siguiente() {
    if (!pasoValido(current)) {
      setAviso("Completa este paso antes de continuar.")
      return
    }
    setAviso(null)
    const destino = Math.min(current + 1, ULTIMO_PASO)
    desbloquearHasta(destino)
    api?.scrollTo(destino)
  }

  // Al activar el switch ya no se simula con un setTimeout: se manda de verdad el POST
  // /productos con los datos capturados en el paso "Producto". Si el backend lo acepta,
  // eso ES la prueba de que "la base de datos detecto" el pago; si falla, se avisa con el
  // mismo Alert que usan los demas formularios.
  function alternarPago(activo: boolean) {
    setPagoActivado(activo)
    if (!activo) {
      setPagoEstado("idle")
      return
    }
    setPagoEstado("procesando")
    crearProductoMutation.mutate(
      { nombre: nombreProducto, sku, precio: Number(precio), stock: Number(stock) },
      {
        onSuccess: (creado) => {
          setProductoCreado(creado)
          setPagoEstado("listo")
        },
        onError: () => {
          setPagoEstado("idle")
          setPagoActivado(false)
        },
      }
    )
  }

  return (
    <section className="max-w-2xl">
      <Button variant="outline" size="sm" asChild className="mb-4">
        <Link to="/">
          <ArrowLeftIcon />
          Volver al menú principal
        </Link>
      </Button>

      <h2>Opción 2 · Formulario de pago</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Carrusel de {PASOS.length} pasos: en cuanto uno queda bien lleno, avanza solo al
        siguiente (no hace falta tocar "Siguiente"). El pago es 100% falso, pero al
        "pagar" sí se crea un producto real en la base de datos -- mismo endpoint que usa
        la página de Productos.
      </p>

      <NavigationMenu viewport={false} className="mb-4 max-w-none justify-start">
        <NavigationMenuList className="flex-wrap justify-start gap-1">
          {PASOS.map((titulo, indice) => {
            const desbloqueado = indice <= maxStep
            const esActual = indice === current
            return (
              <NavigationMenuItem key={titulo}>
                <button
                  type="button"
                  disabled={!desbloqueado}
                  onClick={() => irAPaso(indice)}
                  className={cn(
                    navigationMenuTriggerStyle(),
                    esActual && "bg-muted font-semibold text-foreground",
                    !desbloqueado && "cursor-not-allowed opacity-40"
                  )}
                >
                  <span
                    className={cn(
                      "flex size-5 items-center justify-center rounded-full border text-xs",
                      esActual
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-border"
                    )}
                  >
                    {indice + 1}
                  </span>
                  {titulo}
                </button>
              </NavigationMenuItem>
            )
          })}
        </NavigationMenuList>
      </NavigationMenu>

      <Card>
        <CardContent>
          <Carousel setApi={setApi} opts={{ watchDrag: false }} className="w-full">
            <CarouselContent>
              <CarouselItem>
                <div className="space-y-3">
                  <h3 className="text-sm font-medium">
                    Elige una opción (mutuamente excluyentes)
                  </h3>
                  {OPCIONES_PREFERENCIA.map((op) => (
                    <label
                      key={op.valor}
                      className="flex items-center gap-3 rounded-lg border border-input p-3 hover:bg-muted/50 has-data-checked:border-primary/40 has-data-checked:bg-primary/5"
                    >
                      <Checkbox
                        checked={preferencia === op.valor}
                        onCheckedChange={(marcado) => {
                          setPreferencia(marcado === true ? op.valor : null)
                        }}
                      />
                      <div className="flex flex-col">
                        <span className="text-sm font-medium">{op.titulo}</span>
                        <span className="text-xs text-muted-foreground">
                          {op.descripcion}
                        </span>
                      </div>
                    </label>
                  ))}
                </div>
              </CarouselItem>

              <CarouselItem>
                <Field>
                  <FieldLabel htmlFor="categoria-pago">Categoría</FieldLabel>
                  <NativeSelect
                    id="categoria-pago"
                    value={categoria}
                    onChange={(e) => setCategoria(e.target.value)}
                  >
                    <option value="">Selecciona una categoría…</option>
                    <option value="mensual">Suscripción mensual</option>
                    <option value="anual">Suscripción anual</option>
                    <option value="unico">Pago único</option>
                  </NativeSelect>
                  <FieldDescription>
                    Este es un {"<select>"} nativo del navegador (no el componente Select de
                    Radix).
                  </FieldDescription>
                </Field>
              </CarouselItem>

              <CarouselItem>
                <FieldGroup>
                  <Field>
                    <FieldLabel htmlFor="producto-nombre">Nombre del producto</FieldLabel>
                    <Input
                      id="producto-nombre"
                      value={nombreProducto}
                      onChange={(e) => setNombreProducto(e.target.value)}
                      placeholder="Ej. Plan Premium"
                    />
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="producto-sku">SKU</FieldLabel>
                    <Input
                      id="producto-sku"
                      value={sku}
                      onChange={(e) => setSku(e.target.value)}
                      placeholder="Ej. PLAN-PREM-01"
                    />
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="producto-precio">Precio</FieldLabel>
                    <Input
                      id="producto-precio"
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={precio}
                      onChange={(e) => setPrecio(e.target.value)}
                    />
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="producto-stock">Stock</FieldLabel>
                    <Input
                      id="producto-stock"
                      type="number"
                      step="1"
                      min="0"
                      value={stock}
                      onChange={(e) => setStock(e.target.value)}
                    />
                    <FieldDescription>
                      Estos datos arman el producto que se crea de verdad en la base de
                      datos al "pagar" (mismos campos que usa /productos).
                    </FieldDescription>
                  </Field>
                </FieldGroup>
              </CarouselItem>

              <CarouselItem>
                <Tabs defaultValue="metodo">
                  <TabsList>
                    <TabsTrigger value="metodo">Método</TabsTrigger>
                    <TabsTrigger value="confirmar">Confirmar</TabsTrigger>
                  </TabsList>
                  <TabsContent value="metodo" className="pt-2">
                    <p className="text-sm text-muted-foreground">
                      Tarjeta terminada en •••• 4242 (dato de ejemplo, no real).
                    </p>
                  </TabsContent>
                  <TabsContent value="confirmar" className="pt-2">
                    <div className="flex items-center justify-between rounded-lg border border-input p-3">
                      <div>
                        <p className="text-sm font-medium">Simular pago con tarjeta</p>
                        <p className="text-xs text-muted-foreground">
                          El pago en sí es falso, pero al activar el switch se manda de
                          verdad el producto del paso anterior a la base de datos (POST
                          /productos) -- así se simula que la pasarela le avisa al backend
                          que la compra se completó.
                        </p>
                      </div>
                      <Switch
                        checked={pagoActivado}
                        onCheckedChange={alternarPago}
                        disabled={pagoEstado === "procesando" || crearProductoMutation.isPending}
                      />
                    </div>
                    <div className="mt-3 flex items-center gap-2 text-sm">
                      {pagoEstado === "procesando" && (
                        <>
                          <Spinner /> Registrando el producto en la base de datos…
                        </>
                      )}
                      {pagoEstado === "listo" && productoCreado && (
                        <span className="font-medium text-primary">
                          Pago simulado con éxito -- producto #{productoCreado.id} creado
                          en la base de datos.
                        </span>
                      )}
                      {pagoEstado === "idle" && (
                        <span className="text-muted-foreground">
                          Aún no se ha simulado el pago.
                        </span>
                      )}
                    </div>
                    {crearProductoMutation.isError && (
                      <Alert variant="destructive" className="mt-3">
                        <TriangleAlertIcon />
                        <AlertTitle>No se pudo registrar el pago</AlertTitle>
                        <AlertDescription>
                          {(crearProductoMutation.error as Error).message}
                        </AlertDescription>
                      </Alert>
                    )}
                  </TabsContent>
                </Tabs>
              </CarouselItem>

              <CarouselItem>
                <div className="space-y-4">
                  <Attachment>
                    <AttachmentMedia>
                      <FileTextIcon />
                    </AttachmentMedia>
                    <AttachmentContent>
                      <AttachmentTitle>recibo-simulado.pdf</AttachmentTitle>
                      <AttachmentDescription>
                        Comprobante de ejemplo, generado solo en el navegador.
                      </AttachmentDescription>
                    </AttachmentContent>
                  </Attachment>

                  <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-sm">
                    <dt className="text-muted-foreground">Preferencia</dt>
                    <dd>{preferencia ?? "—"}</dd>
                    <dt className="text-muted-foreground">Categoría</dt>
                    <dd>{categoria || "—"}</dd>
                    <dt className="text-muted-foreground">Producto</dt>
                    <dd>
                      {productoCreado
                        ? `${productoCreado.nombre} (SKU ${productoCreado.sku})`
                        : "—"}
                    </dd>
                    <dt className="text-muted-foreground">Precio / Stock</dt>
                    <dd>
                      {productoCreado
                        ? `$${productoCreado.precio.toFixed(2)} · ${productoCreado.stock} u.`
                        : "—"}
                    </dd>
                    <dt className="text-muted-foreground">Pago</dt>
                    <dd>
                      {productoCreado
                        ? `Registrado en la base de datos (id ${productoCreado.id})`
                        : "Pendiente"}
                    </dd>
                  </dl>

                  {enviado ? (
                    <Alert>
                      <CheckCircle2Icon />
                      <AlertTitle>Enviado</AlertTitle>
                      <AlertDescription>
                        El pago sigue siendo una simulación visual, pero el producto de
                        arriba ya quedó guardado de verdad en la base de datos.
                      </AlertDescription>
                    </Alert>
                  ) : (
                    <Button onClick={() => setEnviado(true)}>Enviar</Button>
                  )}
                </div>
              </CarouselItem>
            </CarouselContent>
          </Carousel>

          {aviso && (
            <Alert variant="destructive" className="mt-4">
              <TriangleAlertIcon />
              <AlertTitle>Falta completar este paso</AlertTitle>
              <AlertDescription>{aviso}</AlertDescription>
            </Alert>
          )}
        </CardContent>

        <CardFooter className="justify-between">
          <Button variant="outline" onClick={anterior} disabled={current === 0}>
            Atrás
          </Button>
          {current < ULTIMO_PASO && <Button onClick={siguiente}>Siguiente</Button>}
        </CardFooter>
      </Card>
    </section>
  )
}

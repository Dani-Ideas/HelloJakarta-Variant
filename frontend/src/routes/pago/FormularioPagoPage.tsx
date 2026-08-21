import { useEffect, useState } from "react"
import { cn } from "@/lib/utils"
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
import { CheckCircle2Icon, FileTextIcon, TriangleAlertIcon } from "lucide-react"

type Preferencia = "opcion1" | "opcion2" | "opcion3"
type EstadoPago = "idle" | "procesando" | "listo"

const PASOS = ["Preferencia", "Categoría", "Datos", "Pago", "Resumen"] as const
const ULTIMO_PASO = PASOS.length - 1

const OPCIONES_PREFERENCIA: { valor: Preferencia; titulo: string; descripcion: string }[] = [
  { valor: "opcion1", titulo: "Opción 1", descripcion: "Plan básico, facturación simple." },
  { valor: "opcion2", titulo: "Opción 2", descripcion: "Plan intermedio, con reportes." },
  { valor: "opcion3", titulo: "Opción 3", descripcion: "Plan completo, todo incluido." },
]

// Formulario de pago simulado: un carrusel de shadcn que SOLO avanza si el paso
// actual quedó válido. Todo es visual -- no hay ningún fetch/POST real.
export function FormularioPagoPage() {
  const [preferencia, setPreferencia] = useState<Preferencia | null>(null)
  const [categoria, setCategoria] = useState("")
  const [nombre, setNombre] = useState("")
  const [pagoActivado, setPagoActivado] = useState(false)
  const [pagoEstado, setPagoEstado] = useState<EstadoPago>("idle")
  const [enviado, setEnviado] = useState(false)

  const [api, setApi] = useState<CarouselApi>()
  const [current, setCurrent] = useState(0)
  const [maxStep, setMaxStep] = useState(0)
  const [aviso, setAviso] = useState<string | null>(null)

  function pasoValido(indice: number): boolean {
    switch (indice) {
      case 0:
        return preferencia !== null
      case 1:
        return categoria !== ""
      case 2:
        return nombre.trim().length > 0
      case 3:
        return pagoEstado === "listo"
      default:
        return true
    }
  }

  // El carrusel se puede mover por swipe/teclado por fuera de nuestros botones --
  // aqui lo "regresamos" si alguien llega (por el motivo que sea) a un paso todavia
  // no desbloqueado, para que la validacion no se pueda saltar.
  useEffect(() => {
    if (!api) return
    function onSelect() {
      const indice = api!.selectedScrollSnap()
      if (indice > maxStep) {
        api!.scrollTo(maxStep)
        return
      }
      setCurrent(indice)
    }
    onSelect()
    api.on("select", onSelect)
    api.on("reInit", onSelect)
    return () => {
      api.off("select", onSelect)
      api.off("reInit", onSelect)
    }
  }, [api, maxStep])

  function irAPaso(indice: number) {
    if (indice > maxStep) return
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
    setMaxStep((m) => Math.max(m, destino))
    api?.scrollTo(destino)
  }

  function alternarPago(activo: boolean) {
    setPagoActivado(activo)
    if (!activo) {
      setPagoEstado("idle")
      return
    }
    setPagoEstado("procesando")
    window.setTimeout(() => setPagoEstado("listo"), 1600)
  }

  return (
    <section className="max-w-2xl">
      <h2>Opción 2 · Formulario de pago</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Carrusel de {PASOS.length} pasos: cada uno debe quedar válido antes de poder ver el
        siguiente. Es una simulación -- nada se guarda en el backend.
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
                    <FieldLabel htmlFor="nombre-pago">Nombre completo</FieldLabel>
                    <Input
                      id="nombre-pago"
                      value={nombre}
                      onChange={(e) => setNombre(e.target.value)}
                      placeholder="Como aparece en tu tarjeta"
                    />
                    <FieldDescription>
                      Dato de ejemplo -- solo se usa dentro de esta simulación.
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
                          Activa el switch para simular el procesamiento (solo visual).
                        </p>
                      </div>
                      <Switch
                        checked={pagoActivado}
                        onCheckedChange={alternarPago}
                        disabled={pagoEstado === "procesando"}
                      />
                    </div>
                    <div className="mt-3 flex items-center gap-2 text-sm">
                      {pagoEstado === "procesando" && (
                        <>
                          <Spinner /> Procesando pago simulado…
                        </>
                      )}
                      {pagoEstado === "listo" && (
                        <span className="font-medium text-primary">
                          Pago simulado con éxito.
                        </span>
                      )}
                      {pagoEstado === "idle" && (
                        <span className="text-muted-foreground">
                          Aún no se ha simulado el pago.
                        </span>
                      )}
                    </div>
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
                    <dt className="text-muted-foreground">Nombre</dt>
                    <dd>{nombre || "—"}</dd>
                    <dt className="text-muted-foreground">Pago</dt>
                    <dd>{pagoEstado === "listo" ? "Simulado" : "Pendiente"}</dd>
                  </dl>

                  {enviado ? (
                    <Alert>
                      <CheckCircle2Icon />
                      <AlertTitle>Enviado</AlertTitle>
                      <AlertDescription>
                        Esto es solo una simulación visual: no se envió nada a ningún
                        servidor.
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

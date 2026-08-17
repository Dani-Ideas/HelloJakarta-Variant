#!/usr/bin/env python3
"""
Hot-swap del frontend: reconstruye React (npm run build) y reemplaza SOLO los archivos
estaticos dentro del WAR ya desplegado y explotado de GlassFish, sin pasar por
`asadmin deploy`. El backend (WEB-INF/clases Java) nunca se toca, asi que GlassFish no
reinicia nada -- la siguiente peticion HTTP simplemente lee del disco los archivos nuevos.

Uso:
    python3 back/scripts/hot_swap_frontend.py
    python3 back/scripts/hot_swap_frontend.py --skip-audit
    python3 back/scripts/hot_swap_frontend.py --deployed-dir /otra/ruta
"""

import argparse
import filecmp
import shutil
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
FRONTEND_DIR = REPO_ROOT / "frontend"
BUILD_OUTPUT = REPO_ROOT / "back" / "src" / "main" / "webapp"
DEPLOYED_APP_DIR_DEFAULT = Path(
    "/home/robute/Documentos/codes/SanboxTEST/glassfish7/glassfish/domains/domain1"
    "/applications/HelloJakarta-variante"
)

# Carpetas del directorio explotado que pertenecen al backend -- nunca se tocan.
BACKEND_PATHS = {"WEB-INF", "META-INF"}


def run(cmd: list[str], cwd: Path) -> None:
    print(f"$ {' '.join(cmd)}  (en {cwd})")
    subprocess.run(cmd, cwd=cwd, check=True)


def npm_audit(skip: bool) -> None:
    if skip:
        print("\n== npm audit omitido (--skip-audit) ==")
        return
    print("\n== npm audit (vulnerabilidades de seguridad en dependencias) ==")
    resultado = subprocess.run(["npm", "audit", "--audit-level=high"], cwd=FRONTEND_DIR)
    if resultado.returncode != 0:
        print("\n!! npm audit encontro vulnerabilidades de nivel alto o superior.")
        respuesta = input("Continuar con el hot-swap de todas formas? [y/N] ")
        if respuesta.strip().lower() != "y":
            sys.exit(1)


def build_frontend() -> None:
    print("\n== npm run build ==")
    run(["npm", "run", "build"], cwd=FRONTEND_DIR)


def listar_archivos(base: Path) -> set[Path]:
    return {p.relative_to(base) for p in base.rglob("*") if p.is_file()}


def sync_a_glassfish(deployed_dir: Path) -> None:
    print(f"\n== Comparando build nuevo vs. lo que GlassFish tiene servido en {deployed_dir} ==")

    if not deployed_dir.exists():
        print(f"!! No existe {deployed_dir} -- ¿ya desplegaste el WAR al menos una vez con asadmin deploy?")
        sys.exit(1)

    nuevos = listar_archivos(BUILD_OUTPUT)
    actuales = {p for p in listar_archivos(deployed_dir) if p.parts[0] not in BACKEND_PATHS}

    agregados = nuevos - actuales
    eliminados = actuales - nuevos
    posibles_cambios = nuevos & actuales
    cambiados = {
        rel
        for rel in posibles_cambios
        if not filecmp.cmp(BUILD_OUTPUT / rel, deployed_dir / rel, shallow=False)
    }

    for rel in sorted(agregados):
        print(f"  + {rel}")
    for rel in sorted(cambiados):
        print(f"  ~ {rel}")
    for rel in sorted(eliminados):
        print(f"  - {rel}")

    if not (agregados or cambiados or eliminados):
        print("  (sin cambios, nada que copiar)")
        return

    for rel in agregados | cambiados:
        destino = deployed_dir / rel
        destino.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(BUILD_OUTPUT / rel, destino)

    for rel in eliminados:
        (deployed_dir / rel).unlink()

    print(
        f"\nListo: {len(agregados)} agregado(s), {len(cambiados)} actualizado(s), "
        f"{len(eliminados)} eliminado(s). El backend (WEB-INF) no se toco -- "
        "sin redeploy, sin reinicio."
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--skip-audit", action="store_true", help="Saltar el npm audit")
    parser.add_argument(
        "--deployed-dir",
        type=Path,
        default=DEPLOYED_APP_DIR_DEFAULT,
        help="Carpeta explotada del WAR ya desplegado en GlassFish",
    )
    args = parser.parse_args()

    npm_audit(skip=args.skip_audit)
    build_frontend()
    sync_a_glassfish(args.deployed_dir)


if __name__ == "__main__":
    main()

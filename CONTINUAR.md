# CONTINUAR — fork Spartan117x/termux-api

Proyecto: actualizar Termux:API sin dejar de darle crédito al original.
Dueño: Hiram (Spartan117x). Ayuda: Muse.

## Dónde estamos (2026-11-19)

- Fork creado y verificado: https://github.com/Spartan117x/termux-api
  Público, forked from termux/termux-api, rama master, 485 commits,
  commit fc26ce17 (2026-09-17). Sin releases/tags todavía en el fork.
- Base verificada en el TANK: Termux de Play v142 + Termux:API de F-Droid
  v0.53.0 sí funcionan juntas. `termux-battery-status` = 78%, cargando
  por USB, 35.0 °C.
- Borradores listos en la VM, SIN subir todavía al fork:
  `~/workspace/termux-api-fork/README-fork.md`,
  `~/workspace/termux-api-fork/CHANGELOG.md`,
  este `CONTINUAR.md`.

## Qué falta para la v0.53.0-hiram.1

1. Hiram aprueba los borradores (README con crédito + CHANGELOG).
2. Subirlos al fork en una rama, por ejemplo `hiram/v0.53.0-hiram.1`,
   NO directo a master. Archivos: README.md (reemplazo), CHANGELOG.md
   (nuevo). No tocar código en este paso.
3. Compilar con GitHub Actions (el workflow `github_action_build` del
   repo). No compilar en la Mint ni en la VM: Gradle está roto en la VM
   y la Mint es de 2 núcleos con GPU dañado.
4. Bajar la APK debug del workflow, instalarla en el TANK encima de la
   de F-Droid solo si la firma lo permite; si no, desinstalar la de
   F-Droid primero y probar: `termux-battery-status`,
   `termux-location -p network -r once`, `termux-notification`.
5. Si pasa la prueba, crear el release `v0.53.0-hiram.1` en GitHub con
   la APK + el código fuente, y el texto del CHANGELOG.
6. Solo después decidir la primera función nueva (candidatos en
   CHANGELOG.md). No meter funciones nuevas en esta primera versión.

## Reglas del fork (no negociar)

- Licencia GPLv3 intacta. Crédito al original arriba del README, con
  link. Versión con sufijo `-hiram.1`, nunca como `v0.54.0` oficial.
- No borrar historial ni autores. Los cambios útiles se proponen de
  vuelta al original como Pull Request.
- Un cambio pequeño por vez, probado en el TANK antes del siguiente.

## Los 10 commits ya en master (v0.53.0...master)

- 760c177 Bump termux-shared a 8aca6dbbf4
- 0a74284 DialogAPI: no devolver resultado dos veces al girar pantalla
- 224d5eb MicRecorderAPI: atrapar todas las excepciones al grabar
- 44dff89 MicRecorderAPI: no agregar mensaje null/vacío
- 36dd27d ResultReturner.isPathInTermuxAppDataDirectory()
- c4dee53 No mostrar "Connection refused" si el servidor API ya murió
- 255bc40 LocationAPI: LocationService nuevo, JSON válido en updates
- ee29d43 LocationAPI: pedir "Allow all the time" en Android 11+
- 3676676 Arreglar sufijo _EXTRA faltante
- fc26ce1 Excepción dedicada por doble returnData()

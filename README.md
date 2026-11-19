# Termux API — fork de Spartan117x

> **Fork mantenido por Hiram Peñaflor Vera ([Spartan117x](https://github.com/Spartan117x)), a partir del proyecto original [termux/termux-api](https://github.com/termux/termux-api) de Termux, por agnostic-apollo y colaboradores.**
>
> Este fork existe para publicar las correcciones que ya están en `master` del original pero que no tienen release desde la v0.53.0 (2025-09-01), y para agregar funciones útiles para Termux en Android 15.
>
> **Crédito:** todo el código original sigue siendo de sus autores, bajo licencia GPLv3. Nada de este repositorio se presenta como trabajo original de Spartan117x, salvo los cambios listados en [CHANGELOG.md](CHANGELOG.md). Si un cambio resulta útil, se propone de vuelta al original como Pull Request.
>
> **Versión de este fork:** `v0.53.0-hiram.1` (en preparación) — base `master` del original, commit `fc26ce17` (2026-09-17), 485 commits, 10 commits después de la v0.53.0.
>
> **Licencia:** [GPLv3](http://www.gnu.org/licenses/gpl-3.0.en.html) — igual que el original. El código fuente de cada release se publica junto con la APK.

[![Build status](https://github.com/termux/termux-api/workflows/Build/badge.svg)](https://github.com/termux/termux-api/actions)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

This is an app exposing Android API to command line usage and scripts or programs.

When developing or packaging, note that this app needs to be signed with the same
key as the main Termux app for permissions to work (only the main Termux app are
allowed to call the API methods in this app).

> **Nota para Termux de Play Store:** el Termux de Play (v142, `googleplay.2026.10.09`) quitó el `sharedUserId`, por eso la Termux:API de F-Droid sí se instala y sí habla con él. Verificado en un Tank 2 Pro con Android 15 el 2026-11-19: `termux-battery-status` devolvió 78%, cargando por USB, 35.0 °C. En el Termux clásico (F-Droid/GitHub) sigue valiendo la regla de siempre: Termux y todos sus plugins deben venir de la misma fuente y con la misma firma.

## Installation

Latest version is `v0.53.0` (original). Versión de este fork en preparación: `v0.53.0-hiram.1`.

Termux:API application can be obtained from [F-Droid](https://f-droid.org/en/packages/com.termux.api/).

Additionally we provide per-commit debug builds for those who want to try
out the latest features or test their pull request. This build can be obtained
from one of the workflow runs listed on [Github Actions](https://github.com/termux/termux-api/actions/workflows/github_action_build.yml?query=branch%3Amaster+event%3Apush)
page.

Signature keys of all offered builds are different. Before you switch the
installation source, you will have to uninstall the Termux application and
all currently installed plugins. Check https://github.com/termux/termux-app#Installation for more info.

## License

Released under the [GPLv3 license](http://www.gnu.org/licenses/gpl-3.0.en.html).

## How API calls are made through the termux-api helper binary

The [termux-api](https://github.com/termux/termux-api-package/blob/master/termux-api.c)
client binary in the `termux-api` package generates two linux anonymous namespace
sockets, and passes their address to the [TermuxApiReceiver broadcast receiver](https://github.com/termux/termux-api/blob/master/app/src/main/java/com/termux/api/TermuxApiReceiver.java)
as in:

```
/system/bin/am broadcast ${BROADCAST_RECEIVER} --es socket_input ${INPUT_SOCKET} --es socket_output ${OUTPUT_SOCKET}
```

The two sockets are used to forward stdin from `termux-api` to the relevant API
class and output from the API class to the stdout of `termux-api`.

## Client scripts

Client scripts which processes command line arguments before calling the
`termux-api` helper binary are available in the [termux-api package](https://github.com/termux/termux-api-package).

## Ideas

- Wifi network search and connect.
- Add extra permissions to the app to (un)install apps, stop processes etc.

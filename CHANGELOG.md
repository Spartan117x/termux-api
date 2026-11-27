# CHANGELOG — fork Spartan117x/termux-api

Formato: lo del original se respeta tal cual; lo de este fork va con fecha y
autor del cambio. Nada se borra del historial original.

## [v0.53.0-spartan117x.2] — 2026-11-26 (base: hiram/v0.53.0-hiram.1)

Segunda versión del fork. Primeras dos funciones nuevas, hechas por Hiram
y Muse. La app pasa a llamarse "Termux API Spartan117x" en el launcher
(el nombre interno del plugin sigue siendo "Termux:API").

### Agregado en este fork

- `termux-battery-history` (método `BatteryHistory`): cada llamada guarda
  una muestra de la batería (nivel %, temperatura °C, estado, si está
  enchufado, fecha/hora) en un archivo JSON privado de la app
  (`battery-history.json`, máx. 288 muestras = 24 h a intervalos de 5 min,
  se podan las viejas) y devuelve el historial completo como JSON. Sin
  servicio en segundo plano: el historial crece con cada llamada; lo ideal
  es llamarlo desde un cron de Termux cada 5 min.
- `termux-salud` (método `Salud`): un solo JSON con batería (nivel,
  temperatura, estado), RAM (total, disponible, usada) y si están prendidos
  los servidores locales de IAs (puertos 8080 heretic, 8081 dolphin y 8082
  wizardlm), por chequeo TCP a 127.0.0.1.

## [v0.53.0-hiram.1] — en preparación (base: master fc26ce17, 2026-09-17)

Primera versión del fork. No cambia funciones a lo loco: publica lo que el
original ya tiene en `master` y no ha sacado en release desde la v0.53.0
(2025-09-01). Son 10 commits, todos de agnostic-apollo en el original:

### Incluido del original (v0.53.0...master)

- `760c177` — Bump termux-shared a 8aca6dbbf4.
- `0a74284` — DialogAPI: no devolver el resultado dos veces si gira la
  pantalla mientras está activo el diálogo de `termux-dialog`.
- `224d5eb` — MicRecorderAPI: atrapar todas las excepciones al empezar a
  grabar, no solo `IllegalStateException` e `IOException`.
- `44dff89` — MicRecorderAPI: no agregar mensaje si es null o vacío.
- `36dd27d` — Extraer a `ResultReturner.isPathInTermuxAppDataDirectory()`
  la comprobación de sockets en el directorio de datos de Termux.
- `c4dee53` — No mostrar la notificación de error
  `java.io.IOException: Connection refused` cuando el proceso del servidor
  API murió antes de recibir el resultado. Requiere el paquete
  `termux-api` v0.60.0.
- `255bc40` — **LocationAPI:** nuevo `LocationService` para
  `termux-location`, con `CountDownLatch` en vez de `Thread.sleep()`.
  Arregla `updates` / `once`: ahora las respuestas múltiples salen como
  un arreglo JSON válido, y se puede correr más de un `termux-location`
  a la vez. El intervalo mínimo pasa de 50 m a 1 m.
- `ee29d43` — LocationAPI: pedir `Allow all the time` para ubicación en
  Android 11+.
- `3676676` — Arreglar el sufijo `_EXTRA` que faltaba en campos agregados
  en c4dee53.
- `fc26ce1` — Lanzar una excepción dedicada y mostrar notificación cuando
  un comando llama dos veces a `ResultReturner.returnData()`.

### Agregado en este fork

- 2026-11-19 — Workflows de GitHub Actions: fijar Java 17
  (`actions/setup-java@v4`, temurin) en `github_action_build.yml` y
  `github_release_build.yml`. Sin esto el runner de GitHub usa Java 25 y
  Gradle 8.9 falla con `Unsupported class file major version 69`.
  El workflow original no fijaba versión de Java.
- (Pendiente de decidir con Hiram) — nada más todavía. Candidatos, en orden:
  1. `termux-battery-history`: guardar historial de batería/temperatura.
  2. `termux-salud`: un solo JSON con batería + temperatura + RAM +
     estado de los servidores locales (puertos 8080/8081/8082).
  3. No tocar nada más en la v0.53.0-hiram.1 hasta probar la build.

### Notas de compatibilidad

- Verificado 2026-11-19 en Tank 2 Pro, Android 15, Termux de Play v142:
  la Termux:API de F-Droid (v0.53.0) sí habla con el Termux de Play.
  `termux-battery-status` devolvió 78%, CHARGING por USB, 35.0 °C.
- En el Termux clásico (F-Droid/GitHub) sigue la regla original: misma
  fuente y misma firma para Termux y todos sus plugins.

## [v0.53.0] — 2025-09-01 (original, termux/termux-api)

- Fix `NullPointerException` in `BatteryStatusAPI` al convertir un
  `Integer` null a `int` para `batteryCurrentNow`, y no mostrar valores
  null. (27a7599)

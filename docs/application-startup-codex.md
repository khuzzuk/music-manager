# Application Startup - Codex Documentation

This document describes the current startup flow for Music Manager. Use it before
changing `MusicManager`, `LoadingScreen`, or service initialization order.

## Purpose

Startup should show a visible loading window before expensive service
initialization runs. The Swing event dispatch thread must remain free long enough
to paint `LoadingScreen`; service construction and index reading run in a
background worker.

## Key Files

- `src/main/java/pl/khuzzuk/MusicManager.java` - application startup, service
  creation, `Context` assembly, loading screen lifecycle, and main window launch.
- `src/main/java/pl/khuzzuk/initialization/LoadingScreen.java` - lightweight
  `JWindow` shown while services initialize.
- `src/main/java/pl/khuzzuk/initialization/LoadingScreenModeler.java` - loading
  label styling.
- `src/main/java/pl/khuzzuk/Context.java` - record collecting initialized
  services for UI classes.

Runtime files touched during startup:

- `index.dat` is created by `MusicManager.createIndexFileIfMissing()` if absent.
- `index.dat` is read by `IndexReaderService.read()` during initialization.
- `metadata-index/` and `playlists/` are service paths passed into metadata and
  playlist services.

## Runtime Flow

`MusicManager.main()` schedules `showLoadingScreenAndInitialize()` on the Swing
event dispatch thread.

Current flow:

1. Create `LoadingScreen` on the EDT.
2. Call `loadingScreen.setVisible(true)` on the EDT so Swing can paint it.
3. Start a `SwingWorker<Context, Void>`.
4. In `doInBackground()`, call `initComponents()` to create services, ensure
   `index.dat` exists, read the current index, and return a populated `Context`.
5. In `done()`, assign `MusicManager.context`, hide and dispose the loading
   screen, then create and show `MainWindow`.
6. If initialization fails, hide the loading screen, show an initialization error
   dialog, dispose the loading screen, and exit with status `-1`.

## Change Contracts

- Keep all `Service` instances created inside `MusicManager.initComponents()` and
  collected into one `Context`.
- Do not perform expensive service initialization directly on the EDT before the
  loading screen has had a chance to paint.
- Keep Swing component creation and visibility changes on the EDT.
- Keep `LoadingScreen` lightweight. It should not depend on services.
- Show `MainWindow` only after `Context` has been fully initialized.

## Known Limitations

- The loading screen currently shows static text only; there is no progress
  reporting during startup.
- Initialization errors are shown as a simple dialog with the exception message.

## Testing Guidance

After changing startup code, run:

```text
.\gradlew.bat compileJava
.\gradlew.bat test
```

Manual check:

- Start the application and verify `LoadingScreen` appears before the main window
  while services and the saved index initialize.

## Minimal Prompt Context

```text
Music Manager startup is owned by `MusicManager`. `main()` schedules
`showLoadingScreenAndInitialize()` on the EDT. That method creates and shows
`LoadingScreen`, then starts a `SwingWorker`. `initComponents()` runs in the
worker background thread and creates all services, ensures `index.dat` exists,
reads the current index, and returns a complete `Context`. In `done()`,
`MusicManager.context` is assigned, the loading screen is hidden/disposed, and
`MainWindow` is shown. Keep service creation in `initComponents()`, keep Swing UI
visibility on the EDT, and do not block the EDT before the loading screen paints.
```

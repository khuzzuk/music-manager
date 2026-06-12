# Media Key Playback - Codex Documentation

This document describes Music Manager's integration with system media keys,
including Bluetooth headset play/pause events such as headset wear detection
when the device exposes those events to Windows.

## Purpose

Music Manager listens for Windows media commands while the application is
running and maps them to the same playback actions used by the player buttons.
This lets headphones that emit media commands pause and resume playback.

The implementation does not read headphone sensor state directly. It reacts only
to media key events delivered by Windows.

## Key Files

- `src/main/java/pl/khuzzuk/ui/WindowsMediaKeyListener.java` - installs a
  Windows low-level keyboard hook through JNA, subclasses the Swing window
  procedure, and detects media key virtual key codes plus `WM_APPCOMMAND`.
- `src/main/java/pl/khuzzuk/ui/MainWindow.java` - creates the listener after
  creating `PlayerPane`, starts it from `addNotify()` after the window is
  displayable, and closes it during window shutdown.
- `src/main/java/pl/khuzzuk/ui/PlayerPane.java` - exposes package-visible
  playback methods used by both button listeners and media key handling.
- `build.gradle` - declares `net.java.dev.jna:jna-platform`, which also brings
  the core JNA dependency.
- `docs/sound-player-codex.md` - broader playback architecture and contracts.

## Entry Points

`WindowsMediaKeyListener.start()` is called by `MainWindow.addNotify()` after
the playback pane is constructed and the Swing window has a native peer. On
non-Windows systems, or before the window is displayable, it returns without
installing a hook or subclassing the window.

Supported Windows virtual key codes:

- `VK_MEDIA_PLAY_PAUSE` -> `PlayerPane.playPause()`
- `VK_MEDIA_STOP` -> `PlayerPane.stop()`
- `VK_MEDIA_NEXT_TRACK` -> `PlayerPane.playNext()`
- `VK_MEDIA_PREV_TRACK` -> `PlayerPane.playPrevious()`

Supported `WM_APPCOMMAND` commands:

- `APPCOMMAND_MEDIA_PLAY` -> `PlayerPane.play()`
- `APPCOMMAND_MEDIA_PAUSE` -> `PlayerPane.pause()`
- `APPCOMMAND_MEDIA_PLAY_PAUSE` -> `PlayerPane.playPause()`
- `APPCOMMAND_MEDIA_STOP` -> `PlayerPane.stop()`
- `APPCOMMAND_MEDIA_NEXTTRACK` -> `PlayerPane.playNext()`
- `APPCOMMAND_MEDIA_PREVIOUSTRACK` -> `PlayerPane.playPrevious()`

The listener schedules all playback actions with `SwingUtilities.invokeLater`
so UI state, button labels, and the progress timer are updated on the Swing EDT.

## Runtime Flow

1. `MainWindow` creates `PlaylistPane`, `PlayerController`, and `PlayerPane`.
2. `MainWindow` creates `WindowsMediaKeyListener` with a small handler that
   delegates to the existing `PlayerPane` methods.
3. `MainWindow.addNotify()` calls `WindowsMediaKeyListener.start()` after Swing
   creates the native window peer.
4. The listener starts a daemon thread named `windows-media-key-listener`.
5. On Windows, the listener subclasses the main window procedure through
   `User32.SetWindowLongPtr` so it can process `WM_APPCOMMAND`.
6. A daemon thread installs `WH_KEYBOARD_LL` through `User32.SetWindowsHookEx`.
7. Keyboard key-down messages are checked for media virtual key codes.
8. `WM_APPCOMMAND` messages are checked for distinct play, pause, play/pause,
   stop, next, and previous commands.
9. `WindowsMediaKeyListener` logs every received native media command to stdout
   with the `[media-key]` prefix and suppresses a second media command that
   arrives within `250ms`. This avoids double-toggling when Windows reports one
   physical media button through both `VK_MEDIA_*` and `WM_APPCOMMAND`.
10. Matching commands are delegated to `PlayerPane` on the Swing EDT.
11. The keyboard hook calls `CallNextHookEx` after handling a key, so it does not consume
   media keys globally or block other applications from receiving them.
12. `MainWindow` calls `WindowsMediaKeyListener.close()` from `windowClosing`,
   which restores the previous window procedure, posts `WM_QUIT` to the listener
   thread, and unhooks Windows.

## Change Contracts

- Keep media key actions routed through `PlayerPane` or `PlayerController`; do
  not call concrete player implementations directly.
- Keep UI mutations on the Swing EDT.
- Keep the listener Windows-only. Other platforms should continue to run without
  native hook setup.
- Start the listener only after the Swing window is displayable. JNA's
  `Native.getComponentPointer(...)` requires a native AWT peer and throws if it
  is called from the frame constructor before `addNotify()`.
- Do not consume media keys unless the product decision changes, because
  consuming them would make Music Manager interfere with browsers or other media
  apps while it is running.
- Keep explicit `PLAY` and `PAUSE` commands separate from `PLAY_PAUSE` toggle
  handling. `PLAY` should be a no-op while already playing, otherwise duplicate
  playback can be started.
- Keep the short duplicate suppression aligned with native event behavior. It is
  intentionally narrow and intended to suppress duplicate reports for one
  physical media button action, not to model player state.
- Debug logging currently writes to stdout from `WindowsMediaKeyListener`,
  `PlayerPane`, and `PlayerController`. Remove or gate these logs before a
  polished release if console noise becomes a problem.
- Close the native hook during shutdown if `MainWindow` lifecycle changes.
- If new media commands are added, update this document and keep button actions
  and media actions aligned.

## Known Limitations

- The feature depends on Windows receiving media key or app-command events from the device.
  If a headset driver does not expose wear detection as media play/pause keys,
  Music Manager cannot observe that sensor state.
- `VK_MEDIA_PLAY_PAUSE` is a toggle command. If a device sends only a toggle
  rather than distinct play and pause commands, Music Manager follows the same
  toggle semantics as the player button.
- The listener is global while the app is running. It does not currently check
  whether Music Manager was the most recent media app.
- Automated coverage is limited because the behavior depends on native Windows
  keyboard hook delivery.

## Testing Guidance

After changing this area, run:

```text
./gradlew.bat compileJava
```

Manual Windows test:

1. Start Music Manager.
2. Add a track to the playlist and start playback.
3. Press a keyboard media play/pause key and verify toggle behavior.
4. Trigger headset wear detection and verify remove maps to pause and wear maps
   to play/resume when the headset exposes those events to Windows.
5. Verify repeated play commands while already playing do not start overlapping
   playback.
6. Verify stop, next, and previous media keys still map to the same behavior as
   the player buttons.
7. For headset debugging, compare console lines:
   `[media-key] received ...`, `[media-key] dispatch ...`, `[player-pane]`, and
   `[player-controller]`. If removing headphones produces no `[media-key]`
   output, Windows is not delivering a media command to the app through the
   supported paths.

## Minimal Prompt Context

```text
Music Manager supports Windows media commands through WindowsMediaKeyListener,
created by MainWindow and started from MainWindow.addNotify() after the frame is
displayable. It uses JNA's WH_KEYBOARD_LL hook for keyboard media keys and
subclasses the Swing window procedure for WM_APPCOMMAND. It logs received native
commands with `[media-key]`, suppresses duplicate media commands that arrive
within 250ms, schedules actions on the Swing EDT, and delegates to PlayerPane.
Keyboard media keys are not consumed globally. The feature reacts to Windows
media events only; it does not read headphone sensor state directly.
```

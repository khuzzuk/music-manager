# SoundPlayer - Codex Documentation

This document is intended as context for Codex when working on playback code in
Music Manager. It describes the player interfaces, implementations, UI
integration, and conventions established while adding MP3 and FLAC playback.

## Files

- `src/main/java/pl/khuzzuk/player/SoundPlayer.java` - common playback interface.
- `src/main/java/pl/khuzzuk/player/MP3Player.java` - MP3 implementation using
  JLayer.
- `src/main/java/pl/khuzzuk/player/FLACPlayer.java` - FLAC implementation using
  direct `org.jflac.FLACDecoder` decoding from `jflac-codec`.
- `src/main/java/pl/khuzzuk/player/WAVPlayer.java` - WAV implementation using
  Java Sound `AudioInputStream` and `SourceDataLine`.
- `src/main/java/pl/khuzzuk/player/OGGPlayer.java` - OGG/Vorbis implementation
  using Java Sound plus VorbisSPI for decoding.
- `src/main/java/pl/khuzzuk/player/SoundPlayerRouter.java` - selects the concrete
  player by `SoundFileType`.
- `src/main/java/pl/khuzzuk/player/SoundFile.java` - path and display title for a
  playable item.
- `src/main/java/pl/khuzzuk/player/PlaylistSoundFile.java` - doubly-linked
  playlist node.
- `src/main/java/pl/khuzzuk/player/SoundFileType.java` - supported file type
  detection by extension.
- `src/main/java/pl/khuzzuk/ui/PlayerController.java` - MVC controller between
  player controls and playlist view.
- `src/main/java/pl/khuzzuk/ui/PlayerPane.java` - playback controls and progress
  slider.
- `src/main/java/pl/khuzzuk/ui/PlaylistPane.java` - playlist view and current
  track selection.
- `src/main/java/pl/khuzzuk/MusicManager.java` - creates the global
  `SoundPlayer` in `initComponents()` and stores it in `Context`.
- `build.gradle` - playback dependencies:
  `javazoom:jlayer:1.0.1`, `org.jflac:jflac-codec:1.5.2`, and
  `dev.mccue:vorbisspi:2024.04.19`.

## Architecture

`SoundPlayer` is the narrow playback abstraction. UI code should depend on
`SoundPlayer` through `PlayerController`, not directly on `MP3Player` or
`FLACPlayer`.

`MusicManager.initComponents()` creates:

```java
SoundPlayer soundPlayer = new SoundPlayerRouter(
        new MP3Player(),
        new FLACPlayer(),
        new WAVPlayer(),
        new OGGPlayer());
```

`SoundPlayerRouter` chooses the concrete player from `SoundFileType.fromPath(...)`.
When adding a new audio format, prefer adding a new concrete `SoundPlayer` and
extending `SoundPlayerRouter` instead of mixing format-specific branches into UI
classes.

## SoundPlayer Contract

Current public API:

```java
void play(SoundFile soundFile);
void pause();
void resume();
void stop();
void seekToMillis(int positionMillis);
void setVolumePercent(int volumePercent);
int getCurrentPositionMillis();
int getCurrentDurationMillis();
```

Expected behavior:

- `play(...)` starts the supplied track from the beginning and replaces any
  current playback.
- `pause()` stores the current position and stops the active decoder/line.
- `resume()` restarts the same track from the stored position.
- `stop()` stops playback, clears active file state, and resets current position
  to `0`.
- `seekToMillis(...)` clamps the requested position into track duration.
- Seeking while playing should resume immediately from the new position.
- Seeking while paused should update the stored position without starting audio.
- Position and duration are exposed in milliseconds for `PlayerPane.progressSlider`.
- `setVolumePercent(...)` clamps the requested volume into `0..100` and applies it
  to current and future playback.
- Implementations should throw `IllegalArgumentException` for invalid
  `SoundFile` input and `IllegalStateException` for playback/read failures.

## Threading Conventions

Playback must not run on the Swing EDT. Each concrete player owns a
single-thread `ExecutorService` with a named daemon thread.

Do not create a new ad hoc `Thread` inside every `play(...)` call. Use the
executor and submit playback work to it.

Use a private lock object for shared playback state. State mutated by playback
threads and UI-triggered methods must be protected by that lock.

Use a monotonically increasing playback session id. If an older playback task
finishes after a newer one has started, it must not clear the newer task's
state.

Closing an active decoder/stream/line is expected during pause, stop, seek, and
track switching. Exceptions caused by expected close operations should not be
reported as playback errors.

## Position Tracking

Track position is stored as milliseconds, not as MP3 frames or byte offsets.

During active playback, position is derived from:

```text
storedPositionMillis + elapsed time since playbackStartNanos
```

On pause, store the current millisecond position before closing the underlying
decoder/line. On resume, convert the stored position into the format-specific
seek representation.

For MP3, JLayer `PlaybackEvent.getFrame()` is not a reliable MP3 frame number in
this project. It reports the audio device position. Do not use it as the primary
source for seek frame state.

## Volume Control

The vertical volume slider in `PlayerPane` is wired through
`PlayerController.setVolumePercent(...)` to the global `SoundPlayer`. The slider
currently initializes playback volume to `60`.

`SoundPlayerRouter` stores the current volume percent, propagates changes to all
concrete players, and reapplies the stored volume before starting a selected
format player. This keeps volume consistent when switching between MP3, FLAC, WAV,
and OGG tracks.

Java Sound based players use `AudioLineVolume` to apply volume to an active
`SourceDataLine`. It prefers `FloatControl.Type.VOLUME` when available and falls
back to `FloatControl.Type.MASTER_GAIN`.

MP3 playback uses `VolumeAwareJavaSoundAudioDevice`, which applies the current
volume when JLayer creates the underlying source line and can update the active
line while playback is running.

## MP3Player Conventions

`MP3Player` uses JLayer `AdvancedPlayer`.

JLayer seeks by MP3 frame number, not by milliseconds. To seek, read the MP3
header with JLayer `Bitstream`/`Header` and use `Header.ms_per_frame()`:

```text
frame = positionMillis / msPerFrame
positionMillis = frame * msPerFrame
```

Do not use a hard-coded frames-per-second constant such as `38`. Frame duration
depends on the actual MP3 header.

`MP3Player` keeps a small `Mp3Info` value with:

- milliseconds per frame;
- estimated duration in milliseconds.

Duration is computed from JLayer header data and file size. Treat this as an
estimate, especially for VBR files.

## FLACPlayer Conventions

`FLACPlayer` uses `org.jflac:jflac-codec` directly:

```java
FLACDecoder decoder = new FLACDecoder(inputStream)
```

Do not route FLAC input through Java Sound SPI (`AudioSystem.getAudioInputStream`).
Java Sound is used only for output through `SourceDataLine`.

JFLAC `decodeFrame(...)` returns PCM bytes in the source stream format. Use
`StreamInfo.getAudioFormat()` for the output line format instead of hand-written
format guesses.

FLAC seeking is implemented by opening a fresh `FLACDecoder`, decoding frames,
and discarding PCM bytes until the requested millisecond position:

```text
targetFrames = positionMillis * frameRate / 1000
targetBytes = targetFrames * frameSize
```

This is simple and dependency-light, but not a true FLAC seek-table seek. If
future performance becomes a problem for long files, keep the public
`SoundPlayer` contract and replace only FLAC internals.

## WAVPlayer Conventions

`WAVPlayer` uses Java Sound to read WAV files:

```java
AudioSystem.getAudioInputStream(...)
```

Playback writes decoded PCM directly to `SourceDataLine`. Seeking is implemented
by opening a fresh stream and skipping bytes derived from the WAV audio format's
frame rate and frame size. This keeps the same millisecond-based public contract
as MP3 and FLAC playback.

## OGGPlayer Conventions

`OGGPlayer` uses Java Sound in the same shape as `WAVPlayer`, but depends on
VorbisSPI to provide OGG/Vorbis decoding through `AudioSystem`.

Open the source stream with:

```java
AudioSystem.getAudioInputStream(...)
```

Then convert it to `PCM_SIGNED` before writing to `SourceDataLine`. Seeking is
implemented by reopening the stream and skipping decoded PCM bytes derived from
frame rate and frame size, matching the millisecond-based `SoundPlayer`
contract used by the other players. Prefer VorbisSPI's `duration` audio file
property for track length, because decoded OGG streams may report an unspecified
frame length.

## UI Integration

`PlayerPane` is a view. It should not read from `PlaylistPane` or call
`SoundPlayer` directly.

`PlayerController` coordinates playback:

- reads the current `SoundFile` from `PlaylistPane`;
- calls `SoundPlayer`;
- tracks play/pause state for the controls;
- forwards volume slider changes to `SoundPlayer.setVolumePercent(...)`;
- advances to the next playlist item when current playback reaches duration.

`PlayerPane.progressSlider` displays milliseconds. A Swing `Timer` refreshes it
while playback is active. Clicking the slider maps x-position to milliseconds and
calls `PlayerController.seekToMillis(...)`.

Playlist navigation is owned by `PlaylistPane`. It uses `PlaylistSoundFile` as a
doubly-linked list and supports looping:

- next after the last item returns to the first item;
- previous before the first item returns to the last item.

## Change Contracts

When changing playback code:

- Keep UI classes format-agnostic.
- Keep `PlayerPane` and `PlaylistPane` separated through `PlayerController`.
- Keep all service/player instances created in `MusicManager.initComponents()`
  and passed through `Context`.
- Preserve pause/resume semantics: resume must continue from the stored
  millisecond position.
- Preserve seek semantics for both playing and paused states.
- Preserve volume as a `0..100` percent value and keep it effective across format
  switches through `SoundPlayerRouter`.
- Preserve progress slider updates in milliseconds.
- Do not block the Swing EDT with decoder or file I/O work.
- Do not let stale playback tasks clear current playback state.
- Prefer adding focused tests around new non-UI state transitions when player
  logic changes; avoid tests that depend on real audio hardware unless isolated
  behind an abstraction.

## Known Limitations

- MP3 duration is estimated from header data and file size; VBR files may be
  imprecise.
- FLAC duration depends on `StreamInfo.totalSamples` and `StreamInfo.sampleRate`.
- FLAC seek skips decoded PCM bytes and may be slow for large target positions.
- OGG playback depends on the Java Sound service provider from VorbisSPI being
  available on the runtime classpath.
- Current players do not expose completion callbacks. `PlayerController` detects
  completion by comparing current position with duration during timer refresh.
- Volume support depends on the active Java Sound line exposing either
  `FloatControl.Type.VOLUME` or `FloatControl.Type.MASTER_GAIN`. If neither
  control is available, the volume request is ignored for that line.

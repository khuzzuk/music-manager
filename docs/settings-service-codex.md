# SettingsService - Codex Documentation

This document is intended as context for Codex when working on the Music Manager
project. It describes the current code behavior, contracts, dependencies, and the
places that usually need to be changed together with `SettingsService`.

The class in this repository is named `SettingsService`, not `SettingService`.

## Files

- `src/main/java/pl/khuzzuk/settings/SettingsService.java` - reads and writes
  settings.
- `src/main/java/pl/khuzzuk/settings/Settings.java` - immutable settings record.
- `src/main/java/pl/khuzzuk/settings/SettingsToPropertiesMapper.java` - maps
  between `Settings` and `java.util.Properties`.
- `settings.properties` - settings file created and overwritten by the service.
- `src/main/java/pl/khuzzuk/MusicManager.java` - creates the global service
  instance.
- `src/main/java/pl/khuzzuk/ui/MainWindow.java` - reads window settings on startup.
- `src/main/java/pl/khuzzuk/ui/CloseAppListener.java` - saves window settings when
  the application closes.

## Service Role

`SettingsService` is a narrow I/O layer for application settings. Its tasks are:

1. Resolve the settings file path.
2. Create an empty settings file if it does not exist.
3. Load `Properties` from the file.
4. Delegate `Properties -> Settings` mapping to `SettingsToPropertiesMapper`.
5. Keep the last loaded settings object in the `settings` field.
6. Save a provided `Settings` object to the file through the mapper.

The service should not contain UI logic, playlist semantics, or default-value
rules. Default values and property key names belong in the mapper.

## Public API

### Constructor

```java
public SettingsService(SettingsToPropertiesMapper settingsToPropertiesMapper) throws IOException
```

Behavior:

- stores the mapper in the `settingsToPropertiesMapper` field;
- immediately calls private `loadSettings()`;
- may throw `IOException` if the settings file cannot be created or read;
- after successful construction, `getSettings()` should return a `Settings`
  object.

### getSettings

```java
public Settings getSettings()
```

Returns the `Settings` object currently held in memory by the service.

Important current limitation: `saveSettings(Settings settings)` writes data to the
file but does not update the `this.settings` field. After saving, `getSettings()`
may still return the state loaded during construction.

### saveSettings

```java
public void saveSettings(Settings settings) throws IOException
```

Behavior:

- maps `Settings` to `Properties`;
- opens `settings.properties` through `Files.newOutputStream(path)`;
- writes the full set with `props.store(out, "Settings")`;
- overwrites the file contents;
- may throw `IOException`.

`Properties.store(...)` adds a comment and timestamp and does not guarantee a
stable logical key order.

## Private I/O Behavior

### loadSettings

```java
private void loadSettings() throws IOException
```

Behavior:

- creates an empty `Properties` object;
- resolves the path through `getSettingsPath()`;
- creates the file with `Files.createFile(path)` if it does not exist;
- loads properties through `Files.newInputStream(path)`;
- maps properties to `Settings`;
- assigns the result to the `settings` field.

An empty file is a valid state. The mapper returns settings with default values in
that case.

### getSettingsPath

```java
private Path getSettingsPath()
```

Returns:

```java
Paths.get("settings.properties")
```

This is a relative path against the process working directory, not necessarily the
project directory. This matters when the application is launched from an IDE,
Gradle, or an external directory.

## Settings Model

`Settings` is a record:

```java
public record Settings(
        int windowX,
        int windowY,
        int windowWidth,
        int windowHeight,
        boolean maximizedWindow,
        String lastTreePosition,
        String lastPlaylist) {
}
```

The record is immutable. Changing one value requires creating a new `Settings`
instance and copying the remaining fields.

## Property Keys

The mapper uses these keys:

| `Settings` field | Key in `settings.properties` | Default value |
| --- | --- | --- |
| `windowX` | `window.x` | `100` |
| `windowY` | `window.y` | `100` |
| `windowWidth` | `window.width` | `600` |
| `windowHeight` | `window.height` | `400` |
| `maximizedWindow` | `window.maximize` | `false` |
| `lastTreePosition` | `last.tree.position` | empty string |
| `lastPlaylist` | `last.playlist` | empty string |

Example:

```properties
#Settings
#Tue May 26 20:58:28 CEST 2026
last.playlist=
last.tree.position=
window.height=1035
window.maximize=false
window.width=2270
window.x=278
window.y=150
```

## Application Flow

1. `MusicManager.initComponents()` creates
   `new SettingsService(new SettingsToPropertiesMapper())`.
2. The service constructor loads or creates `settings.properties`.
3. `MusicManager.showMainWindow()` passes the service to `MainWindow`.
4. `MainWindow` calls `settingsService.getSettings()` and applies the window bounds
   with `setBounds(settings.windowX(), settings.windowY(), settings.windowWidth(), settings.windowHeight())`.
5. `MainWindow` registers `CloseAppListener`.
6. `CloseAppListener.windowClosing(...)` reads the current window `bounds`, combines
   them with the previous `maximizedWindow`, `lastTreePosition`, and `lastPlaylist`
   values, then calls `settingsService.saveSettings(newSettings)`.

## Change Contracts

When changing this area, keep these rules:

- Do not move UI logic into `SettingsService`.
- Do not duplicate property key names outside the mapper unless adding tests or
  documentation.
- Preserve compatibility with existing keys unless the task explicitly requires a
  format migration.
- Do not rely on entry order in `settings.properties`.
- Treat `Settings` as a value object. Create a new record when changing a single
  field.
- If `saveSettings(...)` should update in-memory state, assign `this.settings` only
  after the file write succeeds.
- If adding I/O tests, consider injecting `Path` into the service. The current
  private `Paths.get("settings.properties")` makes test isolation harder.

## Typical Codex Tasks

### Add a New Setting

Change these together:

- `Settings` - add the record field.
- `SettingsToPropertiesMapper` - add the key constant, read with a default value,
  and write to `Properties`.
- Places that call `new Settings(...)`, especially `CloseAppListener`.
- This document - update the key table and application flow if the new field matters
  outside the mapper.

### Refresh In-Memory State After Saving

Current problem: `saveSettings(...)` does not change the `settings` field.

Safe direction:

```java
public void saveSettings(Settings settings) throws IOException {
    Properties props = settingsToPropertiesMapper.toProperties(settings);
    Path path = getSettingsPath();
    try (OutputStream out = Files.newOutputStream(path)) {
        props.store(out, "Settings");
    }
    this.settings = settings;
}
```

Updating the field after the `try` block means memory changes only after a
successful file write.

### Make Testing Easier

The current implementation always uses `settings.properties` from the process
working directory. For unit tests, a constructor or dependency that accepts a
`Path` is more practical: production can still use `Paths.get("settings.properties")`,
while tests can provide a temporary file.

## Known Risks And Weaknesses

- No tests cover file creation, empty files, invalid values, or saving.
- `settingsToPropertiesMapper` is not `final`, even though it is a constructor
  dependency.
- `saveSettings(...)` does not update in-memory state.
- `getSettingsPath()` is private and hard-wired to a relative path.
- `SettingsToPropertiesMapper.getBoolean(...)` catches `NumberFormatException`, but
  `Boolean.parseBoolean(...)` does not throw it. Invalid boolean text returns
  `false`.
- The service is not thread-safe. This currently fits the simple Swing UI flow, but
  callers should not assume safe concurrent writes.

## Minimal Prompt Context

Paste this block when Codex needs to work on settings:

```text
The Music Manager project has `pl.khuzzuk.settings.SettingsService`.
The service reads and writes `settings.properties` from the process working
directory. Its constructor accepts `SettingsToPropertiesMapper`, immediately calls
`loadSettings()`, and may throw `IOException`.

`SettingsService.getSettings()` returns the last loaded `Settings` object.
`SettingsService.saveSettings(Settings)` maps the record to `Properties` and
overwrites the file. Currently, writing the file does not update `this.settings`.

`Settings` is a record:
windowX, windowY, windowWidth, windowHeight, maximizedWindow, lastTreePosition,
lastPlaylist.

Mapper keys:
window.x=windowX default 100
window.y=windowY default 100
window.width=windowWidth default 600
window.height=windowHeight default 400
window.maximize=maximizedWindow default false
last.tree.position=lastTreePosition default ""
last.playlist=lastPlaylist default ""

Integration:
MusicManager creates SettingsService.
MainWindow reads settings and applies window bounds.
CloseAppListener saves bounds when the application closes.

When adding a settings field, change Settings, SettingsToPropertiesMapper, places
that call `new Settings(...)`, and this documentation together.
```

# SettingsService - Codex Documentation

This document is intended as context for Codex when working on the Music Manager
project. It describes the current code behavior, contracts, dependencies, and the
places that usually need to be changed together with `SettingsService`.

The class in this repository is named `SettingsService`, not `SettingService`.

## Files

- `src/main/java/pl/khuzzuk/settings/SettingsService.java` - reads and writes
  settings.
- `src/main/java/pl/khuzzuk/settings/Settings.java` - immutable settings record.
- `src/main/java/pl/khuzzuk/settings/TrackColumn.java` - track-table column name
  and width.
- `src/main/java/pl/khuzzuk/metadata/Tag.java` - supported track-table metadata
  tags, labels, and metadata value providers.
- `src/main/java/pl/khuzzuk/settings/SettingsToPropertiesMapper.java` - maps
  between `Settings` and `java.util.Properties`.
- `settings.properties` - settings file created and overwritten by the service.
- `src/main/java/pl/khuzzuk/MusicManager.java` - creates services and collects
  them into the global `Context` instance.
- `src/main/java/pl/khuzzuk/ui/MainWindow.java` - reads window settings on startup.
- `src/main/java/pl/khuzzuk/ui/MainMenuBar.java` - opens the indexed directory
  dialog from the Index menu.
- `src/main/java/pl/khuzzuk/ui/IndexDirectoriesDialog.java` - lists indexed
  directories and updates indexed path settings.
- `src/main/java/pl/khuzzuk/ui/FileTree.java` - reads `indexedPaths` and displays
  indexed directory names in the tree.
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

After `saveSettings(Settings settings)` succeeds, `getSettings()` returns the saved
object because the service updates `this.settings` after writing the file.

### saveSettings

```java
public void saveSettings(Settings settings) throws IOException
```

Behavior:

- maps `Settings` to `Properties`;
- opens `settings.properties` through `Files.newOutputStream(path)`;
- writes the full set with `props.store(out, "Settings")`;
- overwrites the file contents;
- updates `this.settings` after the file write succeeds;
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
        String lastPlaylist,
        List<Path> indexedPaths,
        Path lastChoosenPath,
        List<TrackColumn> trackColumns) {
}
```

The record is immutable. Changing one value requires creating a new `Settings`
instance and copying the remaining fields.

The compact constructor normalizes nullable path fields:

- `indexedPaths == null` becomes `List.of()`;
- otherwise `indexedPaths` is copied with `List.copyOf(...)`;
- `lastChoosenPath == null` becomes `Path.of("")`.
- `trackColumns == null` becomes `List.of()`;
- otherwise `trackColumns` is copied with `List.copyOf(...)`.

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
| `indexedPaths` | `indexed.paths` | empty string |
| `lastChoosenPath` | `last.choosen.path` | empty path |
| `trackColumns` | `track.columns` | `title:220;duration:80;album:180;composer:160;rating:70;mood:120;movement:120;occasion:120` |

`lastChoosenPath` intentionally uses the current field spelling from code.
`indexedPaths` is stored as one property joined with `File.pathSeparator`, which is
the standard Java separator for lists of paths (`;` on Windows, `:` on Unix-like
systems).
`trackColumns` is stored as one property where each column is `name:width` and
columns are separated with `;`. Column names are stable metadata keys mapped by
`pl.khuzzuk.metadata.Tag`; UI code should use enum constants such as `RATING`,
not raw string literals such as `"rating"`.

Example:

```properties
#Settings
#Tue May 26 20:58:28 CEST 2026
indexed.paths=C\:\\Music;D\:\\Archive\\Music
track.columns=title:220;duration:80;album:180;composer:160;rating:70;mood:120;movement:120;occasion:120
last.playlist=
last.tree.position=
last.choosen.path=C\:\\Music
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
3. `MusicManager.initComponents()` collects the service into `Context`.
4. `MusicManager.showMainWindow()` passes `Context` to `MainWindow`.
5. `MainWindow` calls `context.settingsService().getSettings()` and applies the window bounds
   with `setBounds(settings.windowX(), settings.windowY(), settings.windowWidth(), settings.windowHeight())`.
6. `MainWindow` registers `CloseAppListener`.
7. `MainWindow` creates `MainMenuBar(context)`.
8. `MainMenuBar` opens `IndexDirectoriesDialog` from the Index menu.
9. `IndexDirectoriesDialog` lists current `indexedPaths` and lets the user add a
   directory with `JFileChooser` starting from `lastChoosenPath`.
10. `ContentPane` reads `settings.trackColumns()` and creates `TracksTable` with
   the configured visible metadata columns and widths.
11. `CloseAppListener.windowClosing(...)` reads the current window `bounds`, combines
   them with the previous `maximizedWindow`, `lastTreePosition`, `lastPlaylist`,
   `indexedPaths`, `lastChoosenPath`, and `trackColumns` values, then calls
   `settingsService.saveSettings(newSettings)`.

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
- Keep `saveSettings(...)` updating `this.settings` only after the file write
  succeeds.
- If adding I/O tests, consider injecting `Path` into the service. The current
  private `Paths.get("settings.properties")` makes test isolation harder.

## Typical Codex Tasks

### Add a New Setting

Change these together:

- `Settings` - add the record field.
- `SettingsToPropertiesMapper` - add the key constant, read with a default value,
  and write to `Properties`.
- Places that call `new Settings(...)`, especially `CloseAppListener`.
- UI components that consume the setting, such as `TracksTable` for
  `trackColumns`.
- This document - update the key table and application flow if the new field matters
  outside the mapper.

### Make Testing Easier

The current implementation always uses `settings.properties` from the process
working directory. For unit tests, a constructor or dependency that accepts a
`Path` is more practical: production can still use `Paths.get("settings.properties")`,
while tests can provide a temporary file.

## Known Risks And Weaknesses

- No tests cover file creation, empty files, invalid values, or saving.
- `settingsToPropertiesMapper` is not `final`, even though it is a constructor
  dependency.
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
`SettingsService.saveSettings(Settings)` maps the record to `Properties`,
overwrites the file, and updates `this.settings` after a successful write.

`Settings` is a record:
windowX, windowY, windowWidth, windowHeight, maximizedWindow, lastTreePosition,
lastPlaylist, indexedPaths, lastChoosenPath, trackColumns.

Mapper keys:
window.x=windowX default 100
window.y=windowY default 100
window.width=windowWidth default 600
window.height=windowHeight default 400
window.maximize=maximizedWindow default false
last.tree.position=lastTreePosition default ""
last.playlist=lastPlaylist default ""
indexed.paths=indexedPaths joined with File.pathSeparator default ""
last.choosen.path=lastChoosenPath default empty path
track.columns=trackColumns formatted as name:width entries joined with ; default
title:220;duration:80;album:180;composer:160;rating:70;mood:120;movement:120;occasion:120

Integration:
MusicManager creates SettingsService and stores it in Context.
MainWindow reads settings and applies window bounds.
MainMenuBar receives Context in its constructor and opens
IndexDirectoriesDialog. IndexDirectoriesDialog shows indexedPaths and adds
directories through JFileChooser starting from lastChoosenPath.
ContentPane uses Settings.trackColumns() to configure TracksTable columns.
CloseAppListener saves bounds when the application closes.

When adding a settings field, change Settings, SettingsToPropertiesMapper, places
that call `new Settings(...)`, and this documentation together.
```

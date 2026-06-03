# Track Management UI - Codex Documentation

This document describes the current UI behavior around the tracks table,
metadata editing, and deleting sound files. Use it as implementation context
before changing `TracksTable`, `MetadataEditDialog`, playlist removal behavior,
or track column settings.

## Purpose

The track management UI lets users browse loaded sound files, add them to the
current playlist, edit writable metadata, display file-oriented columns, and
delete selected files from disk. Deletion is coordinated with playback and the
playlist so that the app does not keep playing or listing files after the user
has confirmed their removal.

## Key Files

- `src/main/java/pl/khuzzuk/ui/TracksTable.java` - table view, shortcuts,
  row context menu, editable cells, selected-track actions, file deletion flow,
  and visible row state.
- `src/main/java/pl/khuzzuk/ui/TracksTableContextMenu.java` - row context menu
  composition for add-to-playlist, title-from-file-name, delete, and
  metadata-edit actions.
- `src/main/java/pl/khuzzuk/ui/TracksTableModeler.java` - table, header,
  column menu, and row context menu styling.
- `src/main/java/pl/khuzzuk/ui/TracksTableController.java` - background loading
  of metadata for table rows.
- `src/main/java/pl/khuzzuk/ui/MetadataEditDialog.java` - modal multi-track
  metadata editor.
- `src/main/java/pl/khuzzuk/ui/MetadataEditDialogModeler.java` - styling for the
  metadata editor, including the file path label.
- `src/main/java/pl/khuzzuk/ui/PlaylistPane.java` - current playlist, current
  track pointer, selected playlist row removal, and removal by file path.
- `src/main/java/pl/khuzzuk/ui/PlayerController.java` - playback state and
  stopping playback when a deleted selection contains the active track.
- `src/main/java/pl/khuzzuk/ui/ContentPane.java` - wires `TracksTable` to
  `PlaylistPane` and `PlayerController`.
- `src/main/java/pl/khuzzuk/metadata/Tag.java` - supported table/filter metadata
  tags, including file-oriented read-only values.
- `src/main/java/pl/khuzzuk/settings/SettingsToPropertiesMapper.java` - default
  track columns and persisted column settings.
- `src/test/java/pl/khuzzuk/settings/SettingsToPropertiesMapperTest.java` -
  verifies default/missing track column behavior.

Runtime and generated files:

- `settings.properties` stores `track.columns` as semicolon-separated
  `tag:width` entries.
- `metadata-index/` is updated through `MetadataIndexWriterService` when
  metadata changes or deleted files are removed from the metadata index.
- `index.dat` is not updated by the delete shortcut; reindexing is currently
  needed to remove deleted files from the directory index.

## Entry Points

- `Tab` while `TracksTable` is focused adds selected table tracks to the current
  playlist.
- `Ctrl+Enter` while `TracksTable` is focused opens `MetadataEditDialog` for the
  selected rows.
- `Ctrl+Delete` while `TracksTable` is focused asks for confirmation, then
  deletes selected files.
- Right-clicking a row in `TracksTable` opens a row context menu with
  `Dodaj do playlisty`, `Ustaw tytul z nazwy pliku`, `Usun`, and
  `Edytuj metadane`. Right-clicking an already selected row preserves
  multi-selection; right-clicking an unselected row selects only that row before
  showing the menu.
- `Ustaw tytul z nazwy pliku` writes each selected track's `fileName()` value to
  `Tag.TITLE` through the same metadata write path used by table edits, then
  updates the visible cell and metadata index.
- Direct table cell edits write supported metadata tags immediately.
- Rating cells are updated through the rating renderer/editor mouse interaction.

## Track Columns

`Tag.FILE_NAME` is a supported tag with settings name `fileName`, label
`Nazwa pliku`, and value source `SoundFileMetadata::fileName`.

`SettingsToPropertiesMapper.DEFAULT_TRACK_COLUMNS` includes `FILE_NAME` with a
default width of `240`. When persisted `track.columns` lacks a newly added
default column, `withMissingDefaultColumns(...)` appends missing defaults after
the user-configured columns.

`MetadataWriterService.canWrite(...)` returns `false` for `FILE_NAME` because
`MetadataFieldKeyMapper.toFieldKey(FILE_NAME)` returns `null`. The column is
therefore display-only in `TracksTable`, like format and duration.

## Metadata Editor Path Label

`MetadataEditDialog.createForm(...)` adds a non-editable path label at the top of
the form before writable metadata fields.

Current behavior:

- A single selected file shows `Sciezka:` followed by the full normalized path.
- Multiple selected files show `Sciezki:` and then each full normalized path on
  its own HTML line.
- The label is styled only by `MetadataEditDialogModeler.modelPathLabel(...)`.
- The path label is informational and is not included in `readValues()`.

Keep styling in `MetadataEditDialogModeler`; do not put font, color, border, or
spacing decisions directly into `MetadataEditDialog`.

## Delete Selected Files Flow

`TracksTable.registerDeleteSelectedFilesAction()` binds `Ctrl+Delete` to
`deleteSelectedFiles()`.

Flow after the user confirms deletion:

1. Stop active cell editing if needed.
2. Convert selected view rows to model rows.
3. Build a distinct, normalized list of selected paths.
4. Ask for confirmation with a warning dialog.
5. Call `PlayerController.stopIfActiveSoundFile(selectedPaths)`.
6. Call `PlaylistPane.removeSoundFiles(selectedPaths)` to remove every playlist
   occurrence of each selected file.
7. Delete files in a `SwingWorker` with `Files.delete(path)`.
8. Delete metadata index entries for successfully deleted files.
9. Remove successfully deleted rows from `rowPaths`, `rowMetadata`, and the
   visible table model.
10. Show an error dialog if any selected files failed to delete.

File deletion is intentionally off the Swing EDT. UI state updates happen in the
worker's `done()` method.

## Playback And Playlist Coordination

`PlayerController.stopIfActiveSoundFile(Collection<Path>)` compares the active
`SoundFile` path to the normalized selected paths. If a match is found, it calls
`stop()`, clears `activeSoundFile`, and returns `true`.

`PlaylistPane.removeSoundFiles(Collection<Path>)` normalizes the supplied paths,
walks the linked `PlaylistSoundFile` list, unlinks every matching node, fires a
full table data change, and repaints the playlist. If the removed playlist node
is `currentPlaying`, `unlink(...)` moves `currentPlaying` to the next node when
available, otherwise to the previous node.

The order matters: playback is stopped first, playlist entries are removed
second, and disk deletion happens third.

## Change Contracts

- Keep `TracksTable` format-agnostic; it should work with `Path` and
  `SoundFileMetadata`, not format-specific player classes.
- Keep row context menu actions wired to the same selected-row methods used by
  keyboard shortcuts so add, edit, and delete behavior stays aligned.
- Keep the title-from-file-name action in `TracksTable`, not
  `TracksTableContextMenu`, because it depends on selected rows, metadata writes,
  visible table state, and metadata index updates.
- Keep playback state changes in `PlayerController`, not directly in table UI
  code.
- Keep playlist node mutation inside `PlaylistPane`.
- Keep visual styling for dialogs and UI controls in `*Modeler` classes.
- When adding a `Tag`, update every exhaustive `switch` on `Tag`, especially
  `MetadataFieldKeyMapper` and `SoundFileMetadataUpdateMapper`.
- When adding a default track column, update
  `SettingsToPropertiesMapperTest.readsTrackColumnsFromSettingsProperties()`.
- Do not use `Optional` as a field or parameter type.

## Known Limitations

- `Ctrl+Delete` physically deletes files and cannot undo the operation.
- Failed deletions are reported only by count, not by path list.
- Playlist entries are removed before disk deletion. If a file fails to delete,
  it is not restored to the playlist automatically.
- `index.dat` is not updated by this shortcut. A later reindex is needed to
  remove deleted files from the file tree index.
- The table row is removed only after successful file deletion.
- The metadata editor path label uses HTML text in `JLabel`; very long path
  lists may make the dialog taller and require scrolling.
- Current test coverage focuses on settings column behavior. The delete flow is
  UI-driven and does not have a focused automated test.

## Testing Guidance

After changing track columns, metadata editor layout, or delete flow, run:

```text
.\gradlew.bat compileJava
.\gradlew.bat test
```

Manual checks worth doing in the app:

- Select one or more rows in `TracksTable`, press `Ctrl+Delete`, cancel, and
  verify nothing changes.
- Repeat with confirmation and verify table rows disappear only for deleted
  files.
- Add duplicate occurrences of a track to the playlist, delete it from
  `TracksTable`, and verify every playlist occurrence is removed.
- Start playback for a selected file, delete it, and verify playback stops.
- Open metadata editing with `Ctrl+Enter` for one and multiple files and verify
  the full path label appears above editable fields.
- Enable/disable the `Nazwa pliku` column through the header menu and verify
  width persistence in `settings.properties`.

## Minimal Prompt Context

```text
For Music Manager track management UI changes, read
docs/track-management-ui-codex.md. TracksTable owns row state, shortcuts, the row
context menu, and metadata edits. The TracksTable right-click row menu preserves
multi-selection when clicking an already selected row and exposes add-to-playlist,
title-from-file-name, delete, and metadata-edit actions through selected-row
methods. The title-from-file-name action writes each selected track's `fileName()`
to `Tag.TITLE` via the existing metadata write/update/index flow. Ctrl+Delete confirms deletion, stops playback through
PlayerController when the active file is selected, removes all playlist
occurrences through PlaylistPane, deletes files in a SwingWorker, removes
metadata index entries for successful deletions, then removes table rows.
Tag.FILE_NAME is a read-only column backed by SoundFileMetadata::fileName and
is part of default track columns. MetadataEditDialog shows full normalized paths
at the top as a styled label; styling belongs in MetadataEditDialogModeler.
```

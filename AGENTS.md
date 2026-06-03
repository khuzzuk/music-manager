# Codex project notes

All `Service` instances should be created in `MusicManager.initComponents()` and
collected into a single `Context` instance. Classes that depend on services should
receive the full `Context` through their constructors instead of separate service
parameters.

Object-to-object mapping logic should live in a dedicated Mapper class. For
example, mapping `SoundFileMetadata` to a Lucene `Document` should be handled by a
mapper such as `DocumentMapper`, not inline in a service.

Do not use `Optional` as an object field type or as a method/constructor
parameter type. `Optional` is acceptable as a return type when it clearly models
an absent result.

UI element styling should live in dedicated `Modeler` classes. View classes
should create components, compose layout, and wire behavior, while font, color,
border, opacity, size, margin, and similar visual details should be applied by
a modeler such as `PlayerPaneModeler`.

For UI design work, read:

- `docs/ui-style-guide-codex.md`

That document describes the "Cinematic Classical Archive" visual identity,
palette, typography, component styling rules, and guidance for keeping the UI
modern with a subtle historical flavor.

For documentation work, use:

- `docs/codex-documentation-prompt.md`

When adding or materially changing application features, use the Codex skill:

- `.codex/skills/document-added-features`

That skill documents implemented feature behavior for future Codex sessions.
Also read `docs/codex-documentation-prompt.md` and add or update the relevant
feature documentation under `docs/`.

When working on track table actions, metadata editor UI, track columns, or
delete-file behavior, read:

- `docs/track-management-ui-codex.md`

When working on settings-related code, read:

- `docs/settings-service-codex.md`

That document describes `SettingsService`, `Settings`, `SettingsToPropertiesMapper`,
the `settings.properties` format, integration points, known limitations, and the
usual files that must be changed together.

When working on indexing-related code, read:

- `docs/index-service-codex.md`

That document describes `IndexService`, `IndexItem`, directory/file/unreadable
index nodes, traversal behavior, graceful error handling, and known limitations.

When working on playback-related code, read:

- `docs/sound-player-codex.md`

That document describes `SoundPlayer`, `MP3Player`, `FLACPlayer`,
`SoundPlayerRouter`, playback threading, pause/resume, seek/progress behavior,
MVC boundaries between `PlayerPane` and `PlaylistPane`, and known limitations.

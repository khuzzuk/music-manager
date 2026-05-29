# Codex project notes

All `Service` instances should be created in `MusicManager.initComponents()` and
collected into a single `Context` instance. Classes that depend on services should
receive the full `Context` through their constructors instead of separate service
parameters.

Object-to-object mapping logic should live in a dedicated Mapper class. For
example, mapping `SoundFileMetadata` to a Lucene `Document` should be handled by a
mapper such as `DocumentMapper`, not inline in a service.

For documentation work, use:

- `docs/codex-documentation-prompt.md`

When working on settings-related code, read:

- `docs/settings-service-codex.md`

That document describes `SettingsService`, `Settings`, `SettingsToPropertiesMapper`,
the `settings.properties` format, integration points, known limitations, and the
usual files that must be changed together.

When working on indexing-related code, read:

- `docs/index-service-codex.md`

That document describes `IndexService`, `IndexItem`, directory/file/unreadable
index nodes, traversal behavior, graceful error handling, and known limitations.

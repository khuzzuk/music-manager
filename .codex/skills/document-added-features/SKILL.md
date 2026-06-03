---
name: document-added-features
description: Document newly added or changed application features for future Codex sessions. Use when Codex implements a new feature, materially changes user-facing behavior, adds a workflow, changes persisted data or settings, or updates integration behavior in an application and should record the feature's current behavior, files, contracts, limitations, and testing guidance.
---

# Document Added Features

Use this skill after implementing a feature or meaningful behavior change in an
application repository.

## Workflow

1. Inspect the changed files and nearby callers before writing documentation.
2. Read any existing project documentation that covers the touched area.
3. Add or update a focused document under the repository's documentation folder
   unless the project has a stronger convention.
4. Keep the document tied to the implemented code, not planned architecture.
5. Update project instructions, such as `AGENTS.md`, when future Codex sessions
   need to know that the document exists.
6. Mention verification commands only if they were actually run.

## What To Document

Include the sections that help future implementation work:

- Purpose and current behavior of the feature.
- Key source files, tests, settings files, runtime files, and generated files.
- UI entry points, shortcuts, commands, listeners, or public methods.
- Runtime flow and integration points between views, controllers, services, and
  mappers.
- Persisted formats or data model changes.
- Change contracts: files or behaviors that must stay aligned.
- Known limitations and risks.
- Testing guidance and focused commands.
- Minimal Prompt Context: a short block future Codex sessions can reuse before
  editing the feature.

## Writing Rules

- Use English unless the repository's documentation clearly uses another
  language.
- Prefer concise bullets and precise file paths.
- Do not duplicate large code blocks.
- Do not invent future behavior.
- Do not hide current limitations.
- Preserve unrelated worktree changes.

## Music Manager Notes

For this repository, also read `docs/codex-documentation-prompt.md` before
writing feature documentation. Put new feature docs under `docs/` and add a
short pointer in `AGENTS.md`.

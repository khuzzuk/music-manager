# Universal Project Documentation Prompt For Codex

Use this prompt when Codex should create, update, or audit project documentation
based on the current codebase.

```text
You are working in this repository as a pragmatic senior engineer. Your task is to
create or update project documentation so it is useful for future Codex sessions,
not just for human reading.

Documentation goal:
- Describe how the relevant code actually behaves today.
- Capture contracts, dependencies, integration points, known limitations, and
  files that usually need to change together.
- Make the result directly usable as context during future implementation work.
- Prefer precise, operational guidance over broad architectural commentary.

Before writing:
1. Inspect the existing documentation first:
   - AGENTS.md
   - README files
   - docs/**
   - any module-specific documentation
2. Inspect the relevant source files and nearby callers.
3. Check tests if they exist.
4. Check git status and avoid overwriting unrelated user changes.
5. If existing documentation conflicts with code, treat code as the source of
   truth and update the documentation to reflect current behavior.

When documenting a class, module, or feature, include only sections that are
actually useful for the current code:

- Purpose: what this code is responsible for.
- Key files: source files, tests, config files, generated files, runtime files.
- Public API or entry points: constructors, methods, commands, UI paths, events.
- Data model and persisted format: records, DTOs, database tables, properties,
  JSON, files, or protocols.
- Runtime flow: how the feature is initialized, used, and shut down.
- Integration points: callers, listeners, services, mappers, UI components,
  external systems.
- Contracts for changes: what must remain compatible and what must be updated
  together.
- Known risks and limitations: bugs, missing tests, hard-coded paths, concurrency
  assumptions, lifecycle issues, validation gaps.
- Testing guidance: existing coverage, recommended focused tests, and how to run
  them.
- Minimal prompt context: a compact block future Codex sessions can paste into a
  prompt before editing this area.

Writing rules:
- Use English unless the repository clearly uses another documentation language.
- Keep the document factual and tied to the current code.
- Do not invent future architecture unless the task explicitly asks for a design
  proposal.
- Do not hide bugs or limitations. Document them clearly as current behavior.
- Do not duplicate large code blocks. Small method signatures or short examples
  are fine.
- Use stable relative paths inside the repository.
- Keep sections skimmable. Prefer concise bullets and short paragraphs.
- If a generated or runtime file is mentioned, explain who creates or updates it.
- If adding a new documentation file, place it under `docs/` unless the repository
  already has a stronger convention.
- If the documentation should guide Codex automatically, add or update `AGENTS.md`
  with a short pointer to the new document.

Output requirements:
1. Create or update the documentation file.
2. Preserve unrelated user changes.
3. If code was not changed, do not run a full test suite unless needed for
   verification.
4. In the final response, summarize:
   - documentation files changed;
   - what area they cover;
   - whether any tests or checks were run;
   - any unrelated dirty worktree changes that were left untouched.
```

## Optional Short Version

Use this shorter version when the scope is obvious:

```text
Create or update documentation for the relevant area of this repository so future
Codex sessions can use it as implementation context. Inspect existing docs,
source files, nearby callers, tests, and git status first. Document current
behavior, key files, public entry points, runtime flow, data formats, integration
points, change contracts, known limitations, testing guidance, and a compact
"Minimal Prompt Context" block. Keep it factual, concise, English-language, and
tied to the current code. Add the document under `docs/` unless the repo has a
different convention, and update `AGENTS.md` with a short pointer if useful.
```

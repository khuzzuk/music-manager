# UI Style Guide - Cinematic Classical Archive

Use this document when changing Music Manager UI styling. It gives future Codex
sessions a stable visual direction for modeler classes and shared theme updates.

## Visual Identity

Music Manager should feel like a refined private music archive for orchestral,
classical, historical, and film music. The mood is quiet, noble, and focused:
a dark concert hall combined with a well-organized digital library of scores and
albums.

The interface should support long listening sessions and browsing large
collections without visual fatigue. Avoid theatrical decoration. The historical
flavor should come from restrained material cues, typography, and color accents,
not from ornamental flourishes.

## Palette

Use a balanced dark archive palette:

- Deep charcoal and near-black surfaces for player and navigation chrome.
- Warm ivory text on dark surfaces.
- Muted brass/gold for active playback state, selection, and primary accents.
- Desaturated burgundy or oxblood only as a secondary historical accent.
- Parchment and soft stone surfaces for large data areas that need readability.

Avoid large beige/brown fields, heavy orange, saturated purple, bright blue, and
high-contrast pure black/white combinations. The UI should read as cinematic and
archival, not sepia.

## Typography

- Use clean UI fonts for dense controls and tables.
- Use serif display fonts sparingly for titles or archival headings.
- Use monospaced digits for playback time labels.
- Keep letter spacing at `0`; do not scale font size with viewport width.

## Components

Styling belongs in `*Modeler` classes. Views should only create components,
compose layout, and wire behavior.

Player controls should feel precise and modern:

- Icon buttons should be compact, stable in size, and easy to scan.
- Sliders should use calm custom tracks with a filled active segment and a clear
  thumb.
- Playback progress should be horizontal and visually stronger than volume.
- Volume should remain secondary, vertical, and compact.
- Borders should be hairline or subtle compound borders. Avoid decorative cards,
  nested cards, large shadows, gradient blobs, and ornamental SVG decoration.

## Change Contracts

- Keep `UiTheme` as the shared source for reusable color and font tokens.
- Keep detailed component styling in modelers such as `PlayerPaneModeler`.
- Keep interaction logic in views/controllers, not in modelers.
- When changing the player UI, check both idle and active playback states.
- Run at least `./gradlew.bat compileJava` after Java UI changes.

## Minimal Prompt Context

```text
Use the Cinematic Classical Archive style: a quiet modern private archive for
classical, historical, orchestral, and film music. Prefer deep charcoal surfaces,
warm ivory text, muted brass active states, and sparse burgundy historical
accents. Avoid sepia-heavy beige/brown fields, decorative cards, gradients, and
ornamental flourishes. Put styling in Modeler classes and shared tokens in
UiTheme.
```

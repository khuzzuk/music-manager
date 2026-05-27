# IndexService - Codex Documentation

This document is intended as context for Codex when working on the Music Manager
indexing code. It describes the current tree-building behavior, `IndexItem`
implementations, error handling, and change contracts.

## Files

- `src/main/java/pl/khuzzuk/index/IndexService.java` - builds an `IndexItem` tree
  from one or more filesystem paths.
- `src/main/java/pl/khuzzuk/index/IndexItem.java` - common tree node interface.
- `src/main/java/pl/khuzzuk/index/RootIndexItem.java` - virtual root node named
  `root`.
- `src/main/java/pl/khuzzuk/index/DirectoryIndexItem.java` - directory node with
  mutable children.
- `src/main/java/pl/khuzzuk/index/SoundFileIndexItem.java` - leaf node for files.
- `src/main/java/pl/khuzzuk/index/UnreadableIndexItem.java` - leaf node used when a
  path cannot be read.

## Service Role

`IndexService` scans filesystem paths and returns a tree of `IndexItem` nodes under
a virtual `RootIndexItem`. It is responsible for filesystem traversal only. It does
not currently filter by audio extension, read audio metadata, build Swing tree
nodes, or connect the index to the player.

The current implementation treats every non-directory path as a
`SoundFileIndexItem`.

## Public API

### buildTree(List<Path>)

```java
public RootIndexItem buildTree(List<Path> rootPaths)
```

Builds an `IndexItem` tree under a virtual `RootIndexItem`.

Current behavior:

- does not declare `throws IOException`;
- creates a `RootIndexItem` named `root`;
- normalizes root paths with `toAbsolutePath().normalize()` for filtering;
- removes any root path that is nested under another root path from the same input
  list;
- sorts the remaining root paths by display name with
  `String.CASE_INSENSITIVE_ORDER`;
- maps each path through private `buildItem(path, root)`;
- adds every resulting item to the root's children;
- returns the `RootIndexItem`.

## Internal Flow

Before recursive scanning starts, `buildTree(List<Path>)` filters redundant root
paths inline in its stream pipeline. If the input contains both `C:\Music` and
`C:\Music\Rock`, only `C:\Music` remains as a direct child of `RootIndexItem`;
`Rock` can still appear inside the scanned tree under `Music`.

Filtering delegates each pairwise comparison to `isNestedPath(Path, Path)`, which
uses normalized absolute paths for comparison but keeps the original `Path` object
for scanning and display-name calculation. It does not call `toRealPath()`, so
filtering does not require the path to exist or be readable.

`buildItem(Path path, IndexItem parent)` is the recursive implementation for
non-root nodes.

Current flow:

1. Runs the item-building logic inside one `try` block.
2. Calls `Files.isDirectory(path)`.
3. If the path is not a directory, creates `SoundFileIndexItem`, sets its parent,
   and returns it.
4. If the path is a directory, opens `Files.list(path)`.
5. Creates `DirectoryIndexItem` and sets its parent.
6. Sorts child paths by display name with `String.CASE_INSENSITIVE_ORDER`.
7. Maps each child path through `buildItem(child, item)`.
8. Adds the resulting child list to `DirectoryIndexItem`.
9. If any `IOException` or `SecurityException` occurs during this flow, creates
   `UnreadableIndexItem`, sets its parent, and returns it.

`getName(Path)` uses `path.getFileName().toString()`. If `getFileName()` is `null`
for a root path such as `C:\`, it falls back to `path.toString()`.

## IndexItem Contract

`IndexItem` exposes:

```java
boolean isDirectory();
String getName();
List<IndexItem> getChildren();
IndexItem getParent();
void setParent(IndexItem parent);
boolean isRoot();
```

`isRoot()` is implemented explicitly by each node type. Only `RootIndexItem`
returns `true`.

## Implementations

### RootIndexItem

Represents the virtual root of an index tree.

Behavior:

- `getName()` returns `"root"`;
- `isRoot()` returns `true`;
- `isDirectory()` returns `true` because the root is a container node;
- `getParent()` returns `null`;
- `setParent(...)` is a no-op;
- `getChildren()` returns the mutable backing `List<IndexItem>`;
- children are added by package-private `addChildren(List<IndexItem>)`;
- all paths passed to `IndexService.buildTree(List<Path>)` become direct children
  of this root.

### DirectoryIndexItem

Represents a readable directory.

Behavior:

- `isDirectory()` returns `true`;
- `isRoot()` returns `false`;
- `getChildren()` returns the mutable backing `List<IndexItem>`;
- children are added by package-private `addChildren(List<IndexItem>)`;
- `parent` is set by `IndexService`;
- child nodes receive this directory as their parent during recursive building.

### SoundFileIndexItem

Represents a non-directory filesystem entry.

Behavior:

- `isDirectory()` returns `false`;
- `isRoot()` returns `false`;
- `getChildren()` returns `List.of()`;
- `parent` is set by `IndexService`;
- despite the class name, the current service does not validate whether the file is
  actually an audio file.

### UnreadableIndexItem

Represents a path that could not be read safely.

Behavior:

- `isDirectory()` always returns `false`;
- `isRoot()` returns `false`;
- `getChildren()` returns `List.of()`;
- `parent` is set by `IndexService`;
- used when directory listing fails with `IOException` or access checks fail with
  `SecurityException`.

## Ordering

Root paths and directory children are sorted by `IndexService.getName(path)` using
`String.CASE_INSENSITIVE_ORDER`.

Do not rely on filesystem iteration order. If order matters to UI or tests, rely on
the sorted order from `IndexService`.

## Error Handling

`IndexService` is designed to keep building the tree when a child path is
unreadable.

Current rules:

- `buildTree(...)` and `buildItem(...)` do not throw `IOException`.
- `buildTree(...)` always returns `RootIndexItem`.
- Any unreadable path becomes an `UnreadableIndexItem` leaf with
  `isDirectory() == false`.
- A readable parent directory can still contain unreadable child nodes.
- Missing paths and regular files are currently treated as non-directories and
  become `SoundFileIndexItem`, unless an access check throws `SecurityException`.
- Nested root paths are filtered before scanning and do not become direct children
  of `RootIndexItem`.

## Change Contracts

When changing this area, keep these rules:

- Keep traversal behavior in `IndexService`; do not add Swing-specific tree logic
  here.
- If `IndexItem` changes, update all four implementations:
  `RootIndexItem`, `DirectoryIndexItem`, `SoundFileIndexItem`, and
  `UnreadableIndexItem`.
- If files should be filtered to actual audio files, change `IndexService` and
  document the supported extensions.
- If `SoundFileIndexItem` starts holding metadata or `pl.khuzzuk.player.SoundFile`,
  document the ownership boundary between indexing and playback.
- Preserve parent links unless the caller contract is explicitly changed. Direct
  children of `RootIndexItem` should use that root as their parent.
- Preserve explicit `isRoot()` semantics: only `RootIndexItem` should return
  `true`.
- Preserve nested root filtering unless the task explicitly asks to expose every
  input path as a direct root child.
- Preserve graceful handling of unreadable paths unless the task explicitly asks
  for fail-fast behavior.
- If tests are added, use temporary directories and files instead of project-local
  real paths.

## Known Risks And Weaknesses

- There are no tests for recursive scanning, sorting, parent links, multiple root
  paths, files, or unreadable directories.
- `RootIndexItem.getChildren()` exposes a mutable list.
- `SoundFileIndexItem` currently represents every non-directory path, not only
  sound files.
- `DirectoryIndexItem.getChildren()` exposes a mutable list.
- `UnreadableIndexItem` does not store the exception or reason why reading failed.
- Symbolic links are not handled specially. `Files.isDirectory(path)` follows links
  by default, so linked directory cycles may be a risk if such paths are scanned.
- Missing paths become `SoundFileIndexItem` because `Files.isDirectory(path)`
  returns `false`.

## Testing Guidance

Useful focused tests for this area:

- building a tree for an empty temporary directory;
- building a tree from multiple root paths;
- verifying that a root path nested under another input root path is filtered out;
- building nested directories and files;
- verifying case-insensitive child sorting;
- verifying parent links and `isRoot()` for the virtual root and non-root nodes;
- verifying non-directory root behavior;
- verifying unreadable directory behavior where the platform allows permission
  manipulation.

Run verification with:

```text
.\gradlew.bat test
```

## Minimal Prompt Context

Paste this block when Codex needs to work on indexing:

```text
The Music Manager project has `pl.khuzzuk.index.IndexService`.
`IndexService.buildTree(List<Path>)` returns a `RootIndexItem` and does not declare
`throws IOException`.

The returned root is a virtual `RootIndexItem` with name "root". Every path passed
to `buildTree(List<Path>)` is first compared with other input paths using
`toAbsolutePath().normalize()`. If one input path is nested under another input
path, the nested path is skipped as a direct root child. Remaining paths are sorted
by display name, scanned with buildItem, and added as direct children of the root.

Node interface:
IndexItem has isDirectory(), getName(), getChildren(), getParent(), setParent(),
and isRoot(). isRoot() is explicit: only RootIndexItem returns true.

Implementations:
- RootIndexItem: virtual container, name "root", isRoot true, mutable children.
- DirectoryIndexItem: readable directory, isDirectory true, mutable children.
- SoundFileIndexItem: non-directory leaf, isDirectory false, empty children.
- UnreadableIndexItem: leaf used when reading/listing a path fails.

Traversal:
IndexService recursively scans directories with Files.list(path), sorts children by
display name using String.CASE_INSENSITIVE_ORDER, maps each child through buildItem,
and assigns parent links. If listing a directory throws IOException or
SecurityException, it returns UnreadableIndexItem instead of failing the whole tree.

Current limitations:
SoundFileIndexItem is used for every non-directory path, not only verified audio
files. Missing paths also become SoundFileIndexItem. DirectoryIndexItem exposes a
mutable children list. Symlink cycles are not handled specially.

When changing IndexItem, update RootIndexItem, DirectoryIndexItem,
SoundFileIndexItem, and UnreadableIndexItem together.
```

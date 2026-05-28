# IndexService - Codex Documentation

This document is intended as context for Codex when working on the Music Manager
indexing code. It describes the current tree-building behavior, `IndexItem`
implementations, error handling, and change contracts.

## Files

- `src/main/java/pl/khuzzuk/index/IndexService.java` - builds an `IndexItem` tree
  from one or more filesystem paths and writes the result to `index.dat`.
- `src/main/java/pl/khuzzuk/index/IndexReaderService.java` - reads `index.dat`
  and reconstructs a `RootIndexItem` tree.
- `index.dat` - generated index output written by `IndexService.index(...)`.
- `src/main/java/pl/khuzzuk/index/IndexItem.java` - common tree node interface.
- `src/main/java/pl/khuzzuk/index/RootIndexItem.java` - virtual root node named
  `root`.
- `src/main/java/pl/khuzzuk/index/DirectoryIndexItem.java` - directory node with
  mutable children.
- `src/main/java/pl/khuzzuk/index/SoundFileIndexItem.java` - leaf node for files.
- `src/main/java/pl/khuzzuk/index/UnreadableIndexItem.java` - leaf node used when a
  path cannot be read.
- `src/test/java/pl/khuzzuk/index/IndexServiceTest.java` - focused tests for tree
  building, parent links, ordering, and `index.dat` output.
- `src/test/java/pl/khuzzuk/index/IndexReaderServiceTest.java` - focused tests for
  parsing `index.dat`, unescaping names, and writer/reader round-trip behavior.

## Service Role

`IndexService` scans filesystem paths, returns a tree of `IndexItem` nodes under a
virtual `RootIndexItem`, and writes the resulting tree to `index.dat`.
`IndexReaderService` reads `index.dat` back into the same tree model. The indexing
package is responsible for filesystem traversal, index persistence, and attaching
metadata read by `MetadataReaderService` to sound-file nodes. It does not build
Swing tree nodes or connect the index to the player.

The current implementation treats non-directory paths as `SoundFileIndexItem`
only when their extension is listed in `SoundFileType.EXTENSIONS`.

## Public API

### Constructors

```java
public IndexService(Path indexPath, MetadataReaderService metadataReaderService)
public void addIndexListener(Consumer<RootIndexItem> listener)
public void removeIndexListener(Consumer<RootIndexItem> listener)
```

The constructor injects the output file location and the metadata reader used
when creating sound-file nodes.
Listeners are notified with the supplied `RootIndexItem` after a successful
`index(...)` call and after the index has been written.

### index(RootIndexItem, List<Path>)

```java
public RootIndexItem index(RootIndexItem root, List<Path> rootPaths) throws IOException
```

Synchronizes the supplied `RootIndexItem` with entries found under `rootPaths` and
writes the merged tree to the configured index path.

Hard API assumption: every path in `rootPaths` is a directory. The method documents
this in Javadoc and does not try to support regular-file root paths as first-class
inputs.

Current behavior:

- declares `throws IOException` for `index.dat` write failures;
- mutates and returns the supplied `RootIndexItem`;
- maps root paths through `toAbsolutePath().normalize()`;
- removes exact duplicate normalized root paths;
- removes any normalized root path that is nested under another normalized root
  path from the same input list;
- sorts the remaining root paths by display name with
  `String.CASE_INSENSITIVE_ORDER`;
- removes existing root children that are not present in the current filtered
  `rootPaths`;
- maps each remaining root path through private `mergeDirectory(root, path)`;
- reuses an existing directory node with the same name, compared
  case-insensitively, when merging;
- adds missing directory and supported sound-file nodes, preserving parent links;
- removes existing child nodes from each scanned directory when their names are no
  longer present in the current filesystem listing for that directory;
- sorts children after merging;
- writes the tree to the configured index path;
- returns the `RootIndexItem`.

## Internal Flow

Before recursive scanning starts, `index(RootIndexItem, List<Path>)` maps every
root path through `toAbsolutePath().normalize()`, removes exact duplicates, and
filters redundant root paths inline in its stream pipeline. If the input contains
both `C:\Music` and
`C:\Music\Rock`, only `C:\Music` remains as a direct child of `RootIndexItem`;
`Rock` can still appear inside the scanned tree under `Music`.

Filtering compares normalized absolute paths directly with `Path.startsWith(...)`.
It does not call `toRealPath()`, so filtering does not require the path to exist or
be readable.

`mergeDirectory(IndexItem parent, Path path)` is the recursive implementation for
directory nodes. It either finds an existing `DirectoryIndexItem` with the same
name under `parent` or creates one, then scans the filesystem directory,
removes stale children, and adds missing children into the existing tree.

Current flow:

1. Finds or creates a `DirectoryIndexItem` for the current directory name.
2. Opens `Files.list(path)`.
3. Sorts child paths by display name with `String.CASE_INSENSITIVE_ORDER`.
4. Removes current child nodes whose names are not present in the indexable
   listing. Directories are indexable, and files are indexable only when their
   extension is listed in `SoundFileType.EXTENSIONS`.
5. For each child, `mergeChild(...)` calls `Files.isDirectory(child)`.
6. Directory children recurse through `mergeDirectory(...)`.
7. Supported non-directory children create a `SoundFileIndexItem` only when a
   same-name `SoundFileIndexItem` is not already present under the parent.
   `MetadataReaderService.readMetadata(path)` is called for new sound-file nodes;
   metadata read failures keep the file indexed with empty metadata.
8. After merging a directory, its children are sorted by
   `IndexItem.getName()` with `String.CASE_INSENSITIVE_ORDER`.
9. If a newly-created directory cannot be listed because of `IOException` or
   `SecurityException`, the new directory node is replaced with an
   `UnreadableIndexItem`. If an existing directory cannot be listed, the existing
   tree node is kept as-is.

`getName(Path)` uses `path.getFileName().toString()`. If `getFileName()` is `null`
for a root path such as `C:\`, it falls back to `path.toString()`.

After the tree is built, `saveIndex(IndexItem root)` writes the configured index
path in UTF-8.
The current format is one line per persisted item, in pre-order traversal order.
Directories are marked with a compact `D|` prefix, while sound files are written
as bare escaped full paths. The leading empty line appears before root. Other
empty lines are ignored by the current reader.

```text

D|root
D|escaped-normalized-directory-path
escaped-normalized-file-path

```

`RootIndexItem` is persisted like a directory as `D|root`, with one empty line
before the root line. The file does not include an explicit depth value; hierarchy
is reconstructed from persisted full paths and their parent paths.
`UnreadableIndexItem` nodes are temporary in-memory nodes and are not written to
`index.dat`. There is no `UNKNOWN` marker in the persisted format; any future
non-directory `IndexItem` implementation that is not `UnreadableIndexItem` would
currently be written as a bare escaped path. Persisted values escape backslash,
carriage return, newline, and `|`.

### IndexReaderService

```java
public IndexReaderService(Path indexPath)
public RootIndexItem read() throws IOException
public RootIndexItem getCurrentRootIndexItem()
```

The `Path` constructor injects the input file location. `read()` reads UTF-8
lines, ignores empty lines, expects the first non-empty entry to be `D|root`,
reconstructs the tree with parent links by matching each item path to a persisted
directory parent path, stores the reconstructed root as the current root, and
returns it. `getCurrentRootIndexItem()` returns the most recently read root, or an
empty `RootIndexItem` before the first successful read.

Reader parsing rules:

- `D|root` creates the returned `RootIndexItem`;
- `D|path` creates a `DirectoryIndexItem` from the full path;
- a bare escaped path creates a `SoundFileIndexItem`;
- an item's parent is the existing directory whose path equals the item's
  `Path.getParent()`, or root when no persisted parent directory exists;
- a bare file line before root, a non-root first directory, or an invalid trailing
  escape throws `IOException`.

## IndexItem Contract

`IndexItem` exposes:

```java
boolean isDirectory();
String getName();
Path getPath();
List<IndexItem> getChildren();
boolean hasChildren();
IndexItem getParent();
void setParent(IndexItem parent);
boolean isRoot();
```

`isRoot()` is implemented explicitly by each node type. Only `RootIndexItem`
returns `true`.

`IndexService` creates directory and sound-file items with absolute normalized
paths. `RootIndexItem.getPath()` returns `null`. `UnreadableIndexItem.getPath()`
returns its parent path, because unreadable nodes are temporary placeholders and do
not persist their own readable filesystem path.

## Implementations

### RootIndexItem

Represents the virtual root of an index tree.

Behavior:

- `getName()` returns `"root"`;
- `getPath()` returns `null`;
- `isRoot()` returns `true`;
- `isDirectory()` returns `true` because the root is a container node;
- `getParent()` returns `null`;
- `setParent(...)` is a no-op;
- `getChildren()` returns the mutable backing `List<IndexItem>`;
- `hasChildren()` returns `true` when the backing child list is not empty;
- children are added by package-private `addChildren(List<IndexItem>)`;
- all non-nested paths passed to `IndexService.index(root, rootPaths)` become or
  merge with direct children of this root.

### DirectoryIndexItem

Represents a readable directory.

Behavior:

- `isDirectory()` returns `true`;
- `isRoot()` returns `false`;
- `getPath()` returns the absolute normalized directory path when created by
  `IndexService`; manually created nodes derive it from parent path and name when
  possible;
- `getChildren()` returns the mutable backing `List<IndexItem>`;
- `hasChildren()` returns `true` when the backing child list is not empty;
- children are added by package-private `addChildren(List<IndexItem>)`;
- `parent` is set by `IndexService`;
- child nodes receive this directory as their parent during recursive building.

### SoundFileIndexItem

Represents a supported sound-file filesystem entry.

Behavior:

- `isDirectory()` returns `false`;
- `isRoot()` returns `false`;
- `getPath()` returns the absolute normalized file path when created by
  `IndexService`; manually created nodes derive it from parent path and name when
  possible;
- `getChildren()` returns `List.of()`;
- `hasChildren()` returns `false`;
- `parent` is set by `IndexService`;
- `IndexService` creates this node only for files with extensions listed in
  `SoundFileType.EXTENSIONS`;
- `getMetadata()` returns `SoundFileMetadata` read during indexing, or empty
  metadata when the file was read from `index.dat` or metadata reading failed.

### UnreadableIndexItem

Represents a path that could not be read safely.

Behavior:

- `isDirectory()` always returns `false`;
- `isRoot()` returns `false`;
- `getPath()` returns the parent path;
- `getChildren()` returns `List.of()`;
- `hasChildren()` returns `false`;
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

- `mergeDirectory(...)` and `mergeChild(...)` do not throw `IOException`.
- `index(...)` may throw `IOException` only when writing the configured index file
  fails.
- `index(...)` returns `RootIndexItem` after successful tree merge and file write.
- Any unreadable path becomes an `UnreadableIndexItem` leaf with
  `isDirectory() == false` when it is encountered while creating a missing node.
- A readable parent directory can still contain unreadable child nodes.
- The public `rootPaths` contract assumes directories; missing or regular-file
  root paths are outside the supported API contract.
- Nested root paths are filtered before scanning and do not become direct children
  of `RootIndexItem`.

## Change Contracts

When changing this area, keep these rules:

- Keep traversal behavior in `IndexService`; do not add Swing-specific tree logic
  here.
- If `IndexItem` changes, update all four implementations:
  `RootIndexItem`, `DirectoryIndexItem`, `SoundFileIndexItem`, and
  `UnreadableIndexItem`.
- If supported sound-file extensions change, update `SoundFileType.EXTENSIONS`.
- Keep `SoundFileIndexItem` metadata as indexing-owned `SoundFileMetadata`;
  playback-specific state belongs outside the indexing package.
- Preserve parent links unless the caller contract is explicitly changed. Direct
  children of `RootIndexItem` should use that root as their parent.
- Preserve synchronization semantics for `index(RootIndexItem, List<Path>)`:
  update the supplied tree, avoid duplicating existing same-name directories/files,
  add missing nodes, and remove nodes that are no longer present on disk for the
  scanned root paths/directories.
- If `IndexItem` gains or loses methods such as `hasChildren()`, update every
  implementation and this document together.
- Preserve explicit `isRoot()` semantics: only `RootIndexItem` should return
  `true`.
- Preserve nested root filtering unless the task explicitly asks to expose every
  input path as a direct root child.
- Preserve graceful handling of unreadable paths unless the task explicitly asks
  for fail-fast behavior.
- Preserve configured index-file writing from `index(...)` unless persistence is
  moved to a dedicated component.
- If tests are added, use temporary directories and files instead of project-local
  real paths.

## Known Risks And Weaknesses

- There are tests for `index.dat` output shape, absence of explicit depth, root
  and child ordering, nested-root filtering, merge behavior, parent links, and
  `hasChildren()`.
- There are no tests for unreadable directory behavior because reliable permission
  manipulation is platform-dependent.
- `RootIndexItem.getChildren()` exposes a mutable list.
- `IndexService` validates files by extension only. Metadata read failures do not
  reject a supported file; the node is kept with empty metadata.
- `DirectoryIndexItem.getChildren()` exposes a mutable list.
- `UnreadableIndexItem` does not store the exception or reason why reading failed.
- Symbolic links are not handled specially. `Files.isDirectory(path)` follows links
  by default, so linked directory cycles may be a risk if such paths are scanned.
- Missing or non-directory public root paths are outside the current API contract
  and are not covered by tests.

## Testing Guidance

Existing focused tests in `IndexServiceTest` cover:

- writing root as `D|root` with a leading empty line;
- writing directories with `D|` and files as bare escaped full paths;
- not writing explicit depth values;
- case-insensitive sorting of root paths and children;
- filtering nested root paths;
- merging missing children into an existing supplied root tree;
- removing stale root or directory children that are no longer present on disk;
- avoiding duplicate root directory nodes while merging;
- preserving parent links;
- `hasChildren()` behavior for root, directories, and files.

Existing focused tests in `IndexReaderServiceTest` cover:

- reading `index.dat` into a `RootIndexItem` tree with directory siblings;
- restoring parent links while reading;
- unescaping names;
- reading a tree written by `IndexService`.

Additional useful tests:

- unreadable directory behavior where the platform allows permission manipulation;
- unsupported non-directory root behavior if that contract changes;
- missing root path behavior if that contract changes;
- escaping special characters where the platform allows such file names.

Run verification with:

```text
.\gradlew.bat test
```

## Minimal Prompt Context

Paste this block when Codex needs to work on indexing:

```text
The Music Manager project has `pl.khuzzuk.index.IndexService`.
`IndexService` has an `IndexService(Path indexPath, MetadataReaderService
metadataReaderService)` constructor for injecting the output file and metadata
reader.
`IndexService.index(RootIndexItem, List<Path>)` is the public indexing entry point.
Callers must create and pass the `RootIndexItem`; the service mutates and returns
that supplied root, synchronizing it with current filesystem entries by adding
missing nodes and removing stale nodes. It declares `throws IOException` for
writing the configured index file. Public `rootPaths` are assumed to be
directories.

The supplied root is a virtual `RootIndexItem` with name "root". Every path passed
to `index(RootIndexItem, List<Path>)` is first mapped through
`toAbsolutePath().normalize()`. Exact duplicate normalized paths are removed. If
one normalized input path is nested under another input path, the nested path is
skipped as a direct root child. Remaining paths are sorted by display name and
merged with existing same-name directory nodes where possible.
Missing directory and supported sound-file nodes are added with parent links;
stale nodes not present in the current root paths or scanned indexable directory
listings are removed.
Directory and sound-file items created by `IndexService` expose absolute normalized
paths through `getPath()`. New sound-file items also hold `SoundFileMetadata`
read through `MetadataReaderService`, falling back to empty metadata on read
failure. `UnreadableIndexItem.getPath()` returns its parent path.
After building the tree, IndexService writes `index.dat` as UTF-8 text lines
without explicit depth. Directories, including root, are written as
`D|escaped-normalized-path`; sound files are written as bare escaped normalized
paths. Root is written as `D|root` and is preceded by one empty line. Unreadable
nodes are not persisted.

`IndexReaderService` has an `IndexReaderService(Path indexPath)` constructor and
`read() throws IOException`. It reads `D|...` directory path lines and bare
sound-file path lines into a `RootIndexItem` tree with parent links.

Node interface:
IndexItem has isDirectory(), getName(), getPath(), getChildren(), hasChildren(),
getParent(), setParent(), and isRoot(). isRoot() is explicit: only RootIndexItem
returns true.

Implementations:
- RootIndexItem: virtual container, name "root", isRoot true, mutable children.
- DirectoryIndexItem: readable directory created from a Path, isDirectory true,
  mutable children, getName() derived from path file name.
- SoundFileIndexItem: non-directory leaf created from a Path, isDirectory false,
  empty children, getName() derived from path file name, metadata attached.
- UnreadableIndexItem: leaf used when reading/listing a path fails.

Traversal:
IndexService recursively scans directories with Files.list(path), sorts children by
display name using String.CASE_INSENSITIVE_ORDER, merges same-name directories and
files case-insensitively, adds missing nodes, and assigns parent links. If listing
a newly-created directory throws IOException or SecurityException, it replaces that
new node with UnreadableIndexItem instead of failing the whole tree.

Current limitations:
SoundFileIndexItem is selected by file extension only, not by verified audio
content. Public root paths are assumed to be directories. DirectoryIndexItem
exposes a mutable children list. Symlink cycles are not handled specially.

When changing IndexItem, update RootIndexItem, DirectoryIndexItem,
SoundFileIndexItem, and UnreadableIndexItem together.
```

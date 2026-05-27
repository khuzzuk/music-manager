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
package is responsible for filesystem traversal and index persistence only. It
does not currently filter by audio extension, read audio metadata, build Swing tree
nodes, or connect the index to the player.

The current implementation treats every non-directory path as a
`SoundFileIndexItem`.

## Public API

### Constructors

```java
public IndexService(Path indexPath)
```

The `Path` constructor injects the output file location and is used by tests to
avoid writing a project-local runtime file.

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
- normalizes root paths with `toAbsolutePath().normalize()` for filtering;
- removes any root path that is nested under another root path from the same input
  list;
- sorts the remaining root paths by display name with
  `String.CASE_INSENSITIVE_ORDER`;
- removes existing root children that are not present in the current filtered
  `rootPaths`;
- maps each remaining root path through private `mergeDirectory(root, path)`;
- reuses an existing directory node with the same name, compared
  case-insensitively, when merging;
- adds missing directory and file nodes, preserving parent links;
- removes existing child nodes from each scanned directory when their names are no
  longer present in the current filesystem listing for that directory;
- sorts children after merging;
- writes the tree to the configured index path;
- returns the `RootIndexItem`.

## Internal Flow

Before recursive scanning starts, `index(RootIndexItem, List<Path>)` filters
redundant root paths inline in its stream pipeline. If the input contains both
`C:\Music` and
`C:\Music\Rock`, only `C:\Music` remains as a direct child of `RootIndexItem`;
`Rock` can still appear inside the scanned tree under `Music`.

Filtering delegates each pairwise comparison to `isNestedPath(Path, Path)`, which
uses normalized absolute paths for comparison but keeps the original `Path` object
for scanning and display-name calculation. It does not call `toRealPath()`, so
filtering does not require the path to exist or be readable.

`mergeDirectory(IndexItem parent, Path path)` is the recursive implementation for
directory nodes. It either finds an existing `DirectoryIndexItem` with the same
name under `parent` or creates one, then scans the filesystem directory,
removes stale children, and adds missing children into the existing tree.

Current flow:

1. Finds or creates a `DirectoryIndexItem` for the current directory name.
2. Opens `Files.list(path)`.
3. Sorts child paths by display name with `String.CASE_INSENSITIVE_ORDER`.
4. Removes current child nodes whose names are not present in the listing.
5. For each child, `mergeChild(...)` calls `Files.isDirectory(child)`.
6. Directory children recurse through `mergeDirectory(...)`.
7. Non-directory children create a `SoundFileIndexItem` only when a same-name
   `SoundFileIndexItem` is not already present under the parent.
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
as bare escaped names. Empty lines are structural: the leading empty line appears
before root, and each later empty line closes the current directory on the reader's
stack.

```text

D|root
D|escaped-directory-name
escaped-file-name

```

`RootIndexItem` is persisted like a directory as `D|root`, with one empty line
before the root line. The file does not include an explicit depth value; hierarchy
is derived from entry order and directory-closing empty lines. `UnreadableIndexItem`
nodes are temporary in-memory nodes and are not written to `index.dat`. There is no
`UNKNOWN` marker in the persisted format; any future non-directory `IndexItem`
implementation that is not `UnreadableIndexItem` would currently be written as a
bare escaped name. Names escape backslash, carriage return, newline, and `|`.

### IndexReaderService

```java
public IndexReaderService()
public IndexReaderService(Path indexPath)
public RootIndexItem read() throws IOException
```

The no-arg constructor reads `index.dat` from the process working directory. The
`Path` constructor injects the input file location. `read()` reads UTF-8 lines,
ignores leading empty lines before root, expects the first non-empty entry to be
`D|root`, and reconstructs the tree with parent links.

Reader parsing rules:

- `D|name` creates a directory node and pushes it onto the current directory stack;
- the first `D|root` creates the returned `RootIndexItem`;
- a bare escaped line creates a `SoundFileIndexItem` under the current directory;
- an empty line pops one directory from the stack;
- a bare file line without a current directory, a non-root first directory, or an
  invalid trailing escape throws `IOException`.

## IndexItem Contract

`IndexItem` exposes:

```java
boolean isDirectory();
String getName();
List<IndexItem> getChildren();
boolean hasChildren();
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
- `hasChildren()` returns `true` when the backing child list is not empty;
- children are added by package-private `addChildren(List<IndexItem>)`;
- all non-nested paths passed to `IndexService.index(root, rootPaths)` become or
  merge with direct children of this root.

### DirectoryIndexItem

Represents a readable directory.

Behavior:

- `isDirectory()` returns `true`;
- `isRoot()` returns `false`;
- `getChildren()` returns the mutable backing `List<IndexItem>`;
- `hasChildren()` returns `true` when the backing child list is not empty;
- children are added by package-private `addChildren(List<IndexItem>)`;
- `parent` is set by `IndexService`;
- child nodes receive this directory as their parent during recursive building.

### SoundFileIndexItem

Represents a non-directory filesystem entry.

Behavior:

- `isDirectory()` returns `false`;
- `isRoot()` returns `false`;
- `getChildren()` returns `List.of()`;
- `hasChildren()` returns `false`;
- `parent` is set by `IndexService`;
- despite the class name, the current service does not validate whether the file is
  actually an audio file.

### UnreadableIndexItem

Represents a path that could not be read safely.

Behavior:

- `isDirectory()` always returns `false`;
- `isRoot()` returns `false`;
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
- If files should be filtered to actual audio files, change `IndexService` and
  document the supported extensions.
- If `SoundFileIndexItem` starts holding metadata or `pl.khuzzuk.player.SoundFile`,
  document the ownership boundary between indexing and playback.
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
- `SoundFileIndexItem` currently represents every non-directory path, not only
  sound files.
- `DirectoryIndexItem.getChildren()` exposes a mutable list.
- `UnreadableIndexItem` does not store the exception or reason why reading failed.
- Symbolic links are not handled specially. `Files.isDirectory(path)` follows links
  by default, so linked directory cycles may be a risk if such paths are scanned.
- Missing or non-directory public root paths are outside the current API contract
  and are not covered by tests.

## Testing Guidance

Existing focused tests in `IndexServiceTest` cover:

- writing root as `D|root` with a leading empty line and directory-closing empty
  lines;
- writing directories with `D|` and files as bare escaped names;
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
`IndexService` has an `IndexService(Path indexPath)` constructor for injecting the
output file.
`IndexService.index(RootIndexItem, List<Path>)` is the public indexing entry point.
Callers must create and pass the `RootIndexItem`; the service mutates and returns
that supplied root, synchronizing it with current filesystem entries by adding
missing nodes and removing stale nodes. It declares `throws IOException` for
writing the configured index file. Public `rootPaths` are assumed to be
directories.

The supplied root is a virtual `RootIndexItem` with name "root". Every path passed
to `index(RootIndexItem, List<Path>)` is first compared with other input paths using
`toAbsolutePath().normalize()`. If one input path is nested under another input
path, the nested path is skipped as a direct root child. Remaining paths are sorted
by display name and merged with existing same-name directory nodes where possible.
Missing directory and sound-file nodes are added with parent links; stale nodes
not present in the current root paths or scanned directory listings are removed.
After building the tree, IndexService writes `index.dat` as UTF-8 text lines
without explicit depth. Directories, including root, are written as
`D|escaped-name`; sound files are written as bare escaped names. Root is written as
`D|root` and is preceded by one empty line. Empty lines after directory contents
close the current directory for the reader. Unreadable nodes are not persisted.

`IndexReaderService` has a no-arg constructor for `index.dat`, a
`IndexReaderService(Path indexPath)` constructor, and `read() throws IOException`.
It reads `D|...` directory lines, bare sound-file lines, and empty-line directory
closures into a `RootIndexItem` tree with parent links.

Node interface:
IndexItem has isDirectory(), getName(), getChildren(), hasChildren(), getParent(),
setParent(), and isRoot(). isRoot() is explicit: only RootIndexItem returns true.

Implementations:
- RootIndexItem: virtual container, name "root", isRoot true, mutable children.
- DirectoryIndexItem: readable directory, isDirectory true, mutable children.
- SoundFileIndexItem: non-directory leaf, isDirectory false, empty children.
- UnreadableIndexItem: leaf used when reading/listing a path fails.

Traversal:
IndexService recursively scans directories with Files.list(path), sorts children by
display name using String.CASE_INSENSITIVE_ORDER, merges same-name directories and
files case-insensitively, adds missing nodes, and assigns parent links. If listing
a newly-created directory throws IOException or SecurityException, it replaces that
new node with UnreadableIndexItem instead of failing the whole tree.

Current limitations:
SoundFileIndexItem is used for every non-directory child path, not only verified
audio files. Public root paths are assumed to be directories. DirectoryIndexItem
exposes a mutable children list. Symlink cycles are not handled specially.

When changing IndexItem, update RootIndexItem, DirectoryIndexItem,
SoundFileIndexItem, and UnreadableIndexItem together.
```

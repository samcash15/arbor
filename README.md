# <img src="src/main/resources/images/tree.png" height="32" align="top"> Arbor

**A place where your ideas take root.**

Arbor is a lightweight, modern desktop note-taking and code editing application built with Java and JavaFX. It's designed for developers and writers who want a clean, distraction-free workspace to organize notes, documentation, and code snippets in a local folder-based system — no cloud accounts, no subscriptions, just your files on your machine.

<p align="center">
  <img src="src/main/resources/images/Arbor welcome page.png" alt="Arbor Light Mode" width="49%">
  <img src="src/main/resources/images/Arbor Dark Mode.png" alt="Arbor Dark Mode" width="49%">
</p>

## <img src="src/main/resources/images/leaf.png" height="20" align="top"> Why Arbor?

Most note-taking apps either lock you into proprietary formats, require cloud accounts, or lack proper code editing support. Arbor takes a different approach:

- **Your files, your folders** — Notes are plain files in a directory (called a "grove") on your filesystem. No vendor lock-in, no proprietary formats.
- **Built for developers** — Syntax highlighting, line numbers, and a proper code editor powered by RichTextFX.
- **Markdown-first** — Full markdown rendering with live preview, split-view editing, and a distraction-free writing experience.
- **Fast and local** — A native desktop app with no network dependency. Opens instantly, saves automatically.

## <img src="src/main/resources/images/leaf.png" height="20" align="top"> Features

### Editor
- Multi-tab editing with dirty state tracking and drag-and-drop tab reordering
- Split editor — open files side-by-side with drag-and-drop between panes
- Syntax highlighting for Java, JavaScript/TypeScript, Python, JSON, HTML/XML, and CSS
- Markdown preview with Edit, Split, and Preview view modes
- Find and Replace (Ctrl+F / Ctrl+H) with case sensitivity toggle
- Line numbers and word wrap
- Auto-save after 2 seconds of inactivity
- **Focus Mode** — Dims non-active paragraphs to reduce distractions
- **Typewriter Mode** — Keeps the current line centered in the viewport

### File Management
- Hierarchical file tree with folder/file icons and inline tag display
- Color-coded leaf icons by file type (Java = red-orange, JS = yellow, TS = blue, etc.)
- Create, rename, and delete files and folders via context menu or quick-add button
- Dual-mode search — search by filename or file content (Ctrl+Shift+F)

### Tags
- Mark content with `#hashtag` syntax (e.g., `#meeting`, `#todo`, `#project-x`)
- Tags are highlighted in the editor and displayed inline in the file tree
- Search files by tag with autocomplete (Ctrl+Shift+F, then switch to Tag mode)

### Templates
- Create files from templates via the Command Palette
- Pre-built defaults: Blank Note, Meeting Notes, Bug Report
- Add your own templates in the `~/.arbor/templates` folder

### Daily Notes
- Quickly create or open today's note with **Ctrl+D**
- Auto-creates a dated markdown file (e.g., `2026-02-24.md`) in a configurable folder
- Configurable daily notes folder in settings (default: "Journal")

### Export
- Export notes as HTML or PDF from the Command Palette
- Theme-aware export that respects light/dark mode styling

### Workspace
- **Groves** — Organize work into separate directories, each with its own config
- Switch between recent groves from settings
- **Session restore** — Automatically reopens your previously open tabs on launch
- Window state persistence (size, position, splitter location)
- Status bar with cursor position, word/line count, and mode indicators
- Light and Dark themes

### Command Palette
- **Ctrl+Shift+P** opens a VS Code-style command palette with access to every action in the app
- Fuzzy search across all commands — type any part of a command name or category to filter
- Commands are organized by category (File, Editor, View, Grove) with keyboard shortcut hints
- Execute any command with Enter or click — no need to remember shortcuts
- Works in both light and dark themes

### Keyboard Shortcuts
| Shortcut | Action |
|---|---|
| Ctrl+Shift+P | Command Palette |
| Ctrl+S | Save |
| Ctrl+W | Close tab |
| Ctrl+N | New file |
| Ctrl+Shift+N | New folder |
| Ctrl+D | Daily note |
| Ctrl+Shift+F | Search files |
| Ctrl+Shift+O | Toggle outline panel |
| Ctrl+F | Find in file |
| Ctrl+H | Find and replace |

## <img src="src/main/resources/images/leaf.png" height="20" align="top"> Tech Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Application language |
| **JavaFX 21** | Desktop UI framework |
| **RichTextFX 0.11.5** | Code editor with syntax highlighting |
| **CommonMark 0.24.0** | Markdown parsing and HTML rendering |
| **Gson 2.11.0** | JSON configuration persistence |
| **SLF4J + Logback** | Logging |
| **Maven** | Build and dependency management |

## <img src="src/main/resources/images/leaf.png" height="20" align="top"> Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+

### Build and Run

```bash
mvn clean javafx:run
```

On first launch, Arbor will prompt you to select a directory as your grove. This is where your notes and files will live.

## <img src="src/main/resources/images/leaf.png" height="20" align="top"> Project Structure

```
src/main/java/com/arbor/
├── App.java                  # Application entry point
├── model/                    # Data models (config, grove metadata)
├── service/                  # Business logic (file ops, search, syntax highlighting)
├── util/                     # Helpers (dialogs, icons)
└── view/                     # UI components (editor, toolbar, file tree, etc.)
```

## <img src="src/main/resources/images/leaf.png" height="20" align="top"> Version History

### v0.3.0
- **Tags** — Mark content with `#hashtag` syntax. Tags are highlighted in the editor, displayed inline in the file tree, and searchable via a dedicated tag search mode.
- **Templates** — Create files from built-in or custom templates. Add your own to `~/.arbor/templates`.
- **Daily Notes (Ctrl+D)** — Quickly create or open today's dated note in a configurable journal folder.
- **Split Editor** — Open files side-by-side with drag-and-drop tab support between panes.
- **Focus Mode** — Dims non-active paragraphs to help you concentrate on the current line.
- **Typewriter Mode** — Keeps the active line centered in the editor viewport for a distraction-free writing flow.
- **Export** — Export notes as HTML or PDF with theme-aware styling.
- **Session Restore** — Automatically reopens your previously open tabs when you relaunch Arbor.
- **Status Bar** — Shows cursor position (Ln/Col), word or line count, file type, and active mode indicators.
- **Draggable Tabs** — Reorder tabs by dragging, or drag between split panes to move files.

### v0.2.0
- **Command Palette (Ctrl+Shift+P)** — A searchable command launcher with fuzzy matching, category chips, and shortcut hints. Every action in the app is now discoverable from a single entry point.
- **Dark mode fix** — Fixed file tree root folder name being unreadable in dark theme.

### v0.1.0
- Initial release with multi-tab editor, syntax highlighting, markdown preview, file tree, find/replace, dual-mode search, groves, outline panel, backlinks, and light/dark themes.

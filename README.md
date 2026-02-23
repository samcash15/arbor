# Arbor

**A place where your ideas take root.**

Arbor is a lightweight, modern desktop note-taking and code editing application built with Java and JavaFX. It's designed for developers and writers who want a clean, distraction-free workspace to organize notes, documentation, and code snippets in a local folder-based system — no cloud accounts, no subscriptions, just your files on your machine.

## Why Arbor?

Most note-taking apps either lock you into proprietary formats, require cloud accounts, or lack proper code editing support. Arbor takes a different approach:

- **Your files, your folders** — Notes are plain files in a directory (called a "grove") on your filesystem. No vendor lock-in, no proprietary formats.
- **Built for developers** — Syntax highlighting, line numbers, and a proper code editor powered by RichTextFX.
- **Markdown-first** — Full markdown rendering with live preview, split-view editing, and a distraction-free writing experience.
- **Fast and local** — A native desktop app with no network dependency. Opens instantly, saves automatically.

## Features

### Editor
- Multi-tab editing with dirty state tracking
- Syntax highlighting for Java, JavaScript/TypeScript, Python, JSON, HTML/XML, and CSS
- Markdown preview with Edit, Split, and Preview view modes
- Find and Replace (Ctrl+F / Ctrl+H) with case sensitivity toggle
- Line numbers and word wrap
- Auto-save after 2 seconds of inactivity

### File Management
- Hierarchical file tree with folder/file icons
- Color-coded leaf icons by file type (Java = red-orange, JS = yellow, TS = blue, etc.)
- Create, rename, and delete files and folders via context menu or quick-add button
- Dual-mode search — search by filename or file content (Ctrl+Shift+F)

### Workspace
- **Groves** — Organize work into separate directories, each with its own config
- Switch between recent groves from settings
- Window state persistence (size, position, splitter location)
- Light and Dark themes

### Keyboard Shortcuts
| Shortcut | Action |
|---|---|
| Ctrl+S | Save |
| Ctrl+W | Close tab |
| Ctrl+N | New file |
| Ctrl+Shift+N | New folder |
| Ctrl+Shift+F | Search files |
| Ctrl+F | Find in file |
| Ctrl+H | Find and replace |

## Tech Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Application language |
| **JavaFX 21** | Desktop UI framework |
| **RichTextFX 0.11.5** | Code editor with syntax highlighting |
| **CommonMark 0.24.0** | Markdown parsing and HTML rendering |
| **Gson 2.11.0** | JSON configuration persistence |
| **SLF4J + Logback** | Logging |
| **Maven** | Build and dependency management |

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+

### Build and Run

```bash
mvn clean javafx:run
```

On first launch, Arbor will prompt you to select a directory as your grove. This is where your notes and files will live.

## Project Structure

```
src/main/java/com/arbor/
├── App.java                  # Application entry point
├── model/                    # Data models (config, grove metadata)
├── service/                  # Business logic (file ops, search, syntax highlighting)
├── util/                     # Helpers (dialogs, icons)
└── view/                     # UI components (editor, toolbar, file tree, etc.)
```

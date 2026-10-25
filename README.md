# ThanOS — Desktop Environment Simulator

A desktop environment simulator built with **Java, JavaFX, HTML, CSS, and JavaScript**. ThanOS combines a Java-powered backend with a web-based user interface to recreate the experience of using a modern desktop operating system.

## Overview

ThanOS is a desktop simulation project designed to demonstrate desktop-style user interfaces, application management, window handling, and local data persistence.

The project uses JavaFX WebView to render the frontend and a JavaScript bridge to connect the user interface with Java backend services.

## Features

- **Desktop Interface** — A graphical desktop environment with interactive UI components.
- **Window Management** — Open and manage application windows.
- **Application Manager** — Register and manage available applications.
- **Notes** — Create and manage text notes.
- **Calendar** — Manage calendar events.
- **Calculator** — Perform basic calculations.
- **File Manager** — Browse and interact with files in the application's sandbox.
- **Settings** — Manage supported desktop preferences.
- **Recent Items** — Track recently accessed files and applications.
- **Window History** — Maintain application launch history.
- **Local Persistence** — Store settings, notes, events, recent items, and workspace-related data locally.
- **Sample Resources** — Includes sample files and resources for testing and demonstration.

## Technologies Used

| Technology | Purpose |
|---|---|
| Java | Backend logic and application services |
| JavaFX | Desktop application framework |
| JavaFX WebView | Rendering the web-based interface |
| HTML | User interface structure |
| CSS | Interface styling |
| JavaScript | Frontend interactions |
| JavaScript Bridge | Communication between JavaScript and Java |
| Java Collections | Managing windows, applications, history, and recent items |

## Project Structure

```text
ThanOS/
├── src/                 # Java source code
├── ui/                  # HTML, CSS, and JavaScript interface
├── assets/              # Wallpapers and other visual assets
├── samples/             # Sample files and resources
├── README.md            # Project documentation
├── run.bat              # Windows launch script, if included
└── run.sh               # Unix-like launch script, if included
```

The project may also contain IDE configuration files such as `.project` and `.classpath`.

### Main Backend Components

- `ThanOS.java` — Application entry point and JavaFX initialization.
- `JavaBridge.java` — Connects the JavaScript frontend to Java backend functionality.
- `SystemStore.java` — Coordinates application services and persistent data.
- `AppManager.java` and `AppDefinition.java` — Manage application definitions and registration.
- `WindowManager.java` and `WindowModel.java` — Manage application windows.
- `WindowHistory.java` — Maintains window or application history.
- `RecentFilesQueue.java` — Tracks recent items.
- `NoteManager.java` — Handles note-related operations.
- `CalendarManager.java` — Handles calendar events.
- `SettingsService.java` — Manages settings.
- `WorkspaceManager.java`, `Workspace.java`, `WorkspaceAction.java`, and `WorkspaceQueue.java` — Support workspace-related functionality.

## Requirements

Before running ThanOS, ensure that you have:

- A compatible Java Development Kit (JDK).
- A compatible JavaFX SDK.
- An IDE such as Eclipse or Visual Studio Code, or a terminal.
- Git, if you want to clone or contribute to the repository.

JavaFX modules required by the application include `javafx.controls` and `javafx.web`.

## Installation and Setup

### 1. Clone the repository

```bash
git clone https://github.com/faustinamonishaandrew/ThanOS.git
```

### 2. Open the project

Open the cloned `ThanOS` directory in Eclipse or your preferred Java IDE.

Ensure the IDE recognizes the `src` directory as a Java source folder and that the JavaFX SDK is configured correctly.

### 3. Configure JavaFX

Configure the JavaFX SDK in your IDE and add the required modules.

For a command-line launch, configure the JavaFX module path to point to the `lib` directory of your installed JavaFX SDK.

Example for Windows PowerShell:

```powershell
java --module-path "C:\javafx-sdk\lib" --add-modules javafx.controls,javafx.web -cp bin ThanOS
```

Replace `C:\javafx-sdk\lib` with your actual JavaFX SDK path. Compile the Java source files first if the `bin` directory does not contain the compiled classes.

### 4. Run the application

Run `ThanOS.java` from your IDE after configuring the JavaFX libraries and required VM arguments.

The application loads its user interface from the local `ui/index.html` file.

## Data Storage

ThanOS stores application data locally in a `.thanos` directory under the current user's home directory.

Depending on the enabled functionality, this directory may contain:

- `settings.properties` — Application preferences.
- `notes.txt` — Saved notes.
- `events.txt` — Calendar events.
- `workspaces.txt` — Workspace-related data.
- `recent.txt` — Recent items.
- A sandbox directory — Files used by the simulated file manager.

These are local application files and are separate from the source code stored in GitHub.

## Project Architecture

ThanOS follows a frontend-and-backend architecture:

1. **Frontend:** HTML defines the interface, CSS controls the appearance, and JavaScript handles interactions.
2. **Bridge:** `JavaBridge.java` exposes supported Java functionality to the frontend.
3. **Backend:** Java managers handle application logic, windows, notes, events, settings, and other services.
4. **Persistence:** Local storage services save supported application data between sessions.


## Future Enhancements

Potential improvements include:

- Enhanced window resizing and snapping.
- Additional built-in applications.
- Improved desktop customization.
- More robust file management.
- Better error handling and application recovery.
- Expanded workspace functionality.
- Automated testing and packaging for distribution.


**Repository:** [faustinamonishaandrew/ThanOS](https://github.com/faustinamonishaandrew/ThanOS)

*ThanOS — Exploring desktop environments through Java.*

# ThanOS — Dynamic Workspace Desktop Environment

**Java 21 · JavaFX · Maven · Fully Offline**  
College Data Structures Project

---

## Core Feature: Dynamic Workspaces

ThanOS is built around **genuinely functional workspaces**.  
Each workspace owns its own:

- Open applications (Linked List)
- Window positions and state
- Pending tasks (Queue)

**Example workflow**

```
1. Switch to "Development"
2. Open File Manager + Notes + Task Manager
3. Enqueue a few tasks
4. Switch to "Study"
   → Development windows disappear
   → Study starts clean (or with its own previous apps)
5. Open Calculator + Calendar
6. Switch back to "Development"
   → File Manager, Notes, Task Manager all reappear
   → Tasks still in the queue
```

### Workspace operations
| Action | How |
|--------|-----|
| Switch | Click workspace in right panel |
| Create | **+ Create** button |
| Rename | **Rename** (active workspace) |
| Delete | **Delete** (pick non-active) |
| Go back | **← Previous** (Stack pop) |

---

## Data Structures (real usage)

| Structure | Class | Role |
|-----------|--------|------|
| **HashMap** | `WorkspaceMap` | name → Workspace (O(1) switch) |
| **HashMap** | `AppRegistry` | app id → AppInfo |
| **Linked List** | `ActiveAppList` | running apps *per workspace* |
| **Queue** | `TaskQueue` | pending tasks *per workspace* |
| **Stack** | `HistoryStack` | workspace navigation history |

```
Open App
  → HashMap lookup (AppRegistry + WorkspaceMap)
  → Linked List insert into active workspace
  → Window created & tagged with workspace name

Switch Workspace
  → Stack push current name
  → HashMap get new workspace
  → Hide old windows / show new windows

Enqueue Task
  → Queue of current workspace
```

---

## Desktop Features

- Login screen (PIN `1234` or blank)
- Desktop wallpaper + app icons
- Taskbar: Start, running apps, workspace indicator, clock, battery, Wi-Fi, volume
- Start menu with search
- Right-side Workspace panel
- Desktop widgets: clock, weather (mock), system status
- Dark / light themes

### Applications
File Manager · Notes · Calculator · Task Manager · Calendar · Settings · AI Assistant

Windows support open / close / minimize / maximize / drag / focus / taskbar restore.

---

## Build & Run

```bash
cd ThanOS
mvn clean compile
mvn javafx:run
```

JDK 21+ and Maven 3.8+ required.

---

## Project Layout

```
thanos/
  Main.java
  model/          Workspace, RunningApp, AppInfo, WorkspaceTask, Note
  datastructures/ ActiveAppList, WorkspaceMap, AppRegistry, TaskQueue, HistoryStack
  services/       WorkspaceService, PersistenceService, AssistantService
  ui/             DesktopController, DesktopWindow, LoginScreen
  apps/           FileManager, Notes, Calculator, TaskManager, Calendar, Settings, Assistant
```

---

## Viva points

1. Why each workspace has its **own** Linked List and Queue  
2. How HashMap enables instant workspace switch  
3. How Stack powers “← Previous”  
4. How windows are tagged and filtered on switch (state preservation)  
5. Offline design — File I/O only, mock weather, local assistant  

*The Dynamic Workspace is not a tab strip — it is the operating model of ThanOS.*

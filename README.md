# ThanOS

Java + HTML/CSS/JavaScript desktop environment (JavaFX WebView).

This package is an **updated version of the original ThanOS project**, not a rewrite. Existing apps (Notes, Calendar, Calculator, Settings, lock screen, taskbar, window management) are preserved. Added: real File Manager with sample files, Recent Files queue (max 7), theme wallpapers.

## Requirements

- JDK 21 (or 17+)
- JavaFX SDK 21 (modules: `javafx.controls`, `javafx.graphics`, `javafx.web`)

## Run (Windows)

1. Install JDK 21 and extract the JavaFX SDK (e.g. `C:\javafx-sdk-21.0.6`).
2. Open a Command Prompt in this folder.
3. Either set the environment variable:
   ```
   set JAVAFX_HOME=C:\javafx-sdk-21.0.6
   ```
   or edit `run.bat` if your path differs.
4. Double-click `run.bat` or run it from the prompt.

Compilation uses `src\*.java` (no Maven). The working directory must be this project folder so `ui/`, `assets/`, and `samples/` are found.

## Run (Linux / macOS)

```bash
export JAVAFX_HOME=/path/to/javafx-sdk-21
./run.sh
```

## Unlock

Any non-empty password unlocks the demo lock screen.

## Features preserved / added

| Area | Status |
|------|--------|
| Lock screen, desktop, taskbar, Start menu | Preserved |
| Notes, Calendar, Calculator, Settings | Preserved |
| Window management (LinkedList) | Preserved |
| App registry (HashMap) | Preserved |
| Navigation history (Stack) | Preserved |
| Workspace demo UI | Removed from UI (classes kept for compatibility) |
| Manual "Enqueue / Process Queue" demo | Removed |
| Dark / light theme | Preserved; switches supplied wallpapers |
| File Manager | Enhanced: real files, create/rename/copy/delete, preview |
| Sample files (~15) | Seeded into `~/.thanos/files` on first run only |
| Recent Files (Queue, max 7) | New; persists in `~/.thanos/recent.txt` |

## Data locations

- User data: `~/.thanos/` (settings, notes, events, recent list, file sandbox)
- Bundled samples: `samples/` (copied once into the sandbox if empty)
- Wallpapers: `assets/wallpaper-dark.jpg`, `assets/wallpaper-light.jpg`

Sample files are never overwritten on later launches. User files in the sandbox stay intact.

## Recent Files behaviour

1. Opening a file or launching a built-in app records an entry.
2. Re-opening moves that entry to the newest position (no duplicates).
3. Maximum seven entries; the oldest is dropped when a new distinct item is added.
4. Deleting a file removes its recent entry only (the file itself is deleted only when you confirm Delete).
5. List is restored after restart.
6. Missing files are skipped on load and removed when selected.

Implementation: `RecentFilesQueue` uses `Queue` backed by `LinkedList` (oldest at front, newest at back).

## Data structures (for viva / comments)

- **HashMap** – `AppManager` registry
- **Stack** – `WindowHistory` (back navigation)
- **LinkedList** – `WindowManager` open windows; also backs the recent queue
- **Queue** – `RecentFilesQueue` (seven most recent files/apps)

These names are not shown in the UI.

## Manual testing checklist

1. Compile and launch with `run.bat` / `run.sh`.
2. Unlock with any password.
3. Switch Dark/Light in Settings – wallpaper must change immediately.
4. Open File Manager – see Documents, Pictures, Applications, Profile.json, Sample.java, Schedule.pdf.
5. Double-click a `.txt` – preview and edit; Save.
6. Double-click an image – preview.
7. Double-click `Notes.app` – launches Notes and adds to Recent.
8. Open several files until more than seven; confirm only seven recent entries and oldest dropped.
9. Re-open a recent item – it moves to the top without duplication.
10. Delete a file – confirm dialog; entry leaves Recent.
11. Restart ThanOS – recent list restored; sample files still present; user-created files not wiped.
12. Notes / Calendar / Calculator / Settings still work.

## Project layout

```
ThanOS/
  src/           Java sources
  ui/            index.html, style.css, app.js
  assets/        wallpaper-dark.jpg, wallpaper-light.jpg
  samples/       seed content for File Manager
  run.bat        Windows launch
  run.sh         Unix launch
  README.md
```

## Files modified vs original

**Modified:** `SystemStore.java`, `JavaBridge.java`, `ui/app.js`, `ui/style.css`

**Added:** `RecentFilesQueue.java`, `assets/*`, `samples/**`, `run.bat`, `run.sh`, `README.md`

**Unchanged:** AppDefinition, AppManager, NoteManager, CalendarManager, SettingsService, Window*, Workspace* classes, ThanOS.java, index.html (structure)

Workspace Java classes remain so the original architecture compiles; the UI no longer exposes Workspace or demo queue controls.

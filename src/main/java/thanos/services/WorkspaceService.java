package thanos.services;

import thanos.datastructures.*;
import thanos.model.*;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Core Dynamic Workspace engine.
 *
 * All four data structures participate here:
 *
 *  Open App:
 *    HashMap (WorkspaceMap) → find current workspace
 *    HashMap (AppRegistry)  → look up AppInfo
 *    Linked List            → insert RunningApp into workspace
 *    Stack                  → record history action
 *
 *  Switch Workspace:
 *    Stack push current name
 *    HashMap lookup new workspace
 *    Hide old apps / show new apps (UI layer reacts)
 *
 *  Enqueue Task:
 *    Queue of current workspace
 */
public class WorkspaceService {

    private final WorkspaceMap workspaceMap = new WorkspaceMap();
    private final AppRegistry appRegistry = new AppRegistry();
    private final HistoryStack history = new HistoryStack();

    private String activeName;
    private Consumer<String> onNotify;
    private Consumer<String> onWorkspaceChanged;  // UI callback: new workspace name

    public WorkspaceService() {
        seedApps();
        seedWorkspaces();
    }

    private void seedApps() {
        appRegistry.put("files",    new AppInfo("files",    "File Manager", "📁", "Browse local files"));
        appRegistry.put("notes",    new AppInfo("notes",    "Notes",        "📝", "Create and edit notes"));
        appRegistry.put("calc",     new AppInfo("calc",     "Calculator",   "🔢", "Basic calculator"));
        appRegistry.put("tasks",    new AppInfo("tasks",    "Task Manager", "⚙️", "Processes and task queue"));
        appRegistry.put("calendar", new AppInfo("calendar", "Calendar",     "📅", "Month view calendar"));
        appRegistry.put("settings", new AppInfo("settings", "Settings",     "🎛️", "Theme and preferences"));
        appRegistry.put("assistant",new AppInfo("assistant","AI Assistant", "✦",  "Local offline helper"));
    }

    private void seedWorkspaces() {
        Workspace dev = new Workspace("Development", "Coding and software projects");
        dev.addGoal("Finish module implementation");
        dev.addGoal("Write unit tests");
        dev.addGoal("Review pull requests");
        dev.addFolder("~/projects/ThanOS");
        dev.addFolder("~/projects/libs");
        dev.setContinueHint("Open File Manager → projects, then Notes for the task list.");
        workspaceMap.put("Development", dev);

        Workspace study = new Workspace("Study", "Learning and coursework");
        study.addGoal("Complete DSA chapter exercises");
        study.addGoal("Revise OS notes");
        study.addGoal("Prepare viva talking points");
        study.addFolder("~/Documents/College");
        study.addFolder("~/Documents/Notes");
        study.setContinueHint("Open Notes and continue the last lecture summary.");
        workspaceMap.put("Study", study);

        Workspace research = new Workspace("Research", "Reading and information gathering");
        research.addGoal("Summarize 2 research papers");
        research.addGoal("Update literature table");
        research.addFolder("~/Documents/Papers");
        research.setContinueHint("Open File Manager → Papers, then Notes for summaries.");
        workspaceMap.put("Research", research);

        Workspace personal = new Workspace("Personal", "Everyday personal workspace");
        personal.addGoal("Plan weekly schedule");
        personal.addGoal("Clear pending reminders");
        personal.addFolder("~/Documents");
        personal.addFolder("~/Downloads");
        personal.setContinueHint("Open Calendar or Notes to pick up where you left off.");
        workspaceMap.put("Personal", personal);

        activeName = "Personal";
        personal.startSession();
        history.push("Personal");
    }

    // ---- callbacks ----
    public void setNotifyHandler(Consumer<String> h) { onNotify = h; }
    public void setWorkspaceChangedHandler(Consumer<String> h) { onWorkspaceChanged = h; }

    private void notify(String m) { if (onNotify != null) onNotify.accept(m); }

    // ---- getters ----
    public WorkspaceMap getWorkspaceMap() { return workspaceMap; }
    public AppRegistry getAppRegistry() { return appRegistry; }
    public HistoryStack getHistory() { return history; }
    public String getActiveName() { return activeName; }

    public Workspace getActive() {
        return workspaceMap.get(activeName);
    }

    // ================================================================
    // WORKSPACE CRUD
    // ================================================================

    public Workspace createWorkspace(String name, String description) {
        if (name == null || name.isBlank()) {
            notify("Name cannot be empty");
            return null;
        }
        name = name.trim();
        if (workspaceMap.contains(name)) {
            notify("Workspace already exists: " + name);
            return null;
        }
        Workspace ws = new Workspace(name, description != null ? description : "");
        workspaceMap.put(name, ws);
        notify("Created workspace: " + name);
        return ws;
    }

    public boolean renameWorkspace(String oldName, String newName) {
        if (newName == null || newName.isBlank()) return false;
        newName = newName.trim();
        if (workspaceMap.contains(newName)) {
            notify("Name already taken: " + newName);
            return false;
        }
        boolean ok = workspaceMap.rename(oldName, newName);
        if (ok) {
            if (activeName.equalsIgnoreCase(oldName)) activeName = newName;
            // fix history entries
            notify("Renamed to: " + newName);
        }
        return ok;
    }

    public boolean deleteWorkspace(String name) {
        if (workspaceMap.size() <= 1) {
            notify("Cannot delete the last workspace");
            return false;
        }
        if (name.equalsIgnoreCase(activeName)) {
            notify("Switch to another workspace before deleting the active one");
            return false;
        }
        boolean ok = workspaceMap.remove(name);
        if (ok) notify("Deleted workspace: " + name);
        return ok;
    }

    /**
     * Switch to a different workspace.
     * Pushes current name onto the history Stack, then activates the new one.
     * UI layer receives onWorkspaceChanged callback to hide/show windows.
     */
    public boolean switchWorkspace(String name) {
        if (!workspaceMap.contains(name)) {
            notify("Workspace not found: " + name);
            return false;
        }
        if (name.equalsIgnoreCase(activeName)) return true;

        // end work-time session on current workspace
        Workspace current = getActive();
        if (current != null) current.endSession();

        history.push(activeName);
        activeName = workspaceMap.get(name).getName();
        Workspace next = getActive();
        if (next != null) next.startSession();

        notify("Switched to: " + activeName);
        if (onWorkspaceChanged != null) onWorkspaceChanged.accept(activeName);
        return true;
    }

    /** Pop history Stack to return to the previous workspace. */
    public boolean goBack() {
        while (!history.isEmpty()) {
            String prev = history.pop();
            if (prev != null && !prev.equalsIgnoreCase(activeName)
                    && workspaceMap.contains(prev)) {
                Workspace current = getActive();
                if (current != null) current.endSession();
                activeName = workspaceMap.get(prev).getName();
                Workspace next = getActive();
                if (next != null) next.startSession();
                notify("Returned to: " + activeName);
                if (onWorkspaceChanged != null) onWorkspaceChanged.accept(activeName);
                return true;
            }
        }
        notify("No previous workspace");
        return false;
    }

    /** Add a goal to the active workspace. */
    public void addGoalToActive(String goal) {
        Workspace w = getActive();
        if (w == null || goal == null || goal.isBlank()) return;
        w.addGoal(goal);
        notify("Goal added to " + activeName);
    }

    /** Mark / remove a completed goal from the active workspace. */
    public void completeGoal(String goal) {
        Workspace w = getActive();
        if (w == null) return;
        if (w.removeGoal(goal)) notify("Goal completed: " + goal);
    }

    /** Add a related folder label/path to the active workspace. */
    public void addFolderToActive(String folder) {
        Workspace w = getActive();
        if (w == null || folder == null || folder.isBlank()) return;
        w.addFolder(folder);
        notify("Folder linked to " + activeName);
    }

    /** Update the continue-from hint for the active workspace. */
    public void setContinueHint(String hint) {
        Workspace w = getActive();
        if (w == null) return;
        w.setContinueHint(hint);
    }

    // ================================================================
    // APPLICATION LIFECYCLE (inside active workspace)
    // ================================================================

    /**
     * Open an application inside the active workspace.
     * HashMap lookup → Linked List insert → Stack record.
     */
    public RunningApp openApp(String appId) {
        Workspace ws = getActive();
        if (ws == null) return null;

        AppInfo info = appRegistry.get(appId);
        if (info == null) {
            notify("Unknown app: " + appId);
            return null;
        }

        String instanceId = UUID.randomUUID().toString().substring(0, 8);
        RunningApp app = new RunningApp(instanceId, info.getId(), info.getName(), info.getGlyph());

        // demote other focused apps in this workspace
        for (RunningApp a : ws.getApps().toList()) {
            if (a.getState() == RunningApp.AppState.FOCUSED)
                a.setState(RunningApp.AppState.RUNNING);
        }

        ws.getApps().insert(app);
        // update continue-from so user knows where they left off next time
        ws.setContinueHint("Continue with " + info.getName()
                + "  ·  " + ws.appCount() + " app(s) open in this workspace.");
        notify("Opened " + info.getName() + " in " + activeName);
        return app;
    }

    public void closeApp(String instanceId) {
        Workspace ws = getActive();
        if (ws == null) return;
        RunningApp app = ws.getApps().find(instanceId);
        if (app != null) {
            ws.getApps().remove(instanceId);
            notify("Closed " + app.getAppName());
        }
    }

    public void focusApp(String instanceId) {
        Workspace ws = getActive();
        if (ws == null) return;
        for (RunningApp a : ws.getApps().toList()) {
            if (a.getState() == RunningApp.AppState.FOCUSED)
                a.setState(RunningApp.AppState.RUNNING);
        }
        RunningApp target = ws.getApps().find(instanceId);
        if (target != null) target.setState(RunningApp.AppState.FOCUSED);
    }

    public void minimizeApp(String instanceId) {
        Workspace ws = getActive();
        if (ws == null) return;
        RunningApp a = ws.getApps().find(instanceId);
        if (a != null) a.setState(RunningApp.AppState.MINIMIZED);
    }

    public void restoreApp(String instanceId) {
        Workspace ws = getActive();
        if (ws == null) return;
        RunningApp a = ws.getApps().find(instanceId);
        if (a != null) a.setState(RunningApp.AppState.RUNNING);
    }

    public List<RunningApp> getActiveApps() {
        Workspace ws = getActive();
        return ws != null ? ws.getApps().toList() : List.of();
    }

    // ================================================================
    // TASK QUEUE (per workspace)
    // ================================================================

    public WorkspaceTask enqueueTask(String description) {
        Workspace ws = getActive();
        if (ws == null) return null;
        String id = UUID.randomUUID().toString().substring(0, 6);
        WorkspaceTask t = new WorkspaceTask(id, description);
        ws.getTasks().enqueue(t);
        notify("Task queued in " + activeName);
        return t;
    }

    public WorkspaceTask processNextTask() {
        Workspace ws = getActive();
        if (ws == null) return null;
        WorkspaceTask t = ws.getTasks().dequeue();
        if (t == null) {
            notify("Task queue empty");
            return null;
        }
        t.setStatus(WorkspaceTask.Status.DONE);
        notify("Completed: " + t.getDescription());
        return t;
    }

    public List<WorkspaceTask> getPendingTasks() {
        Workspace ws = getActive();
        return ws != null ? ws.getTasks().all() : List.of();
    }
}

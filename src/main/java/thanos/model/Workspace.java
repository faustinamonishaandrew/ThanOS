package thanos.model;

import thanos.datastructures.ActiveAppList;
import thanos.datastructures.TaskQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * A Dynamic Workspace — the core unit of ThanOS.
 *
 * Each workspace owns:
 *  - ActiveAppList (Linked List) of running applications
 *  - TaskQueue of pending tasks
 *  - Goals still to complete
 *  - Related folders for this context
 *  - Continue-from hint (where to pick up previous work)
 *  - Accumulated work time
 */
public class Workspace {
    private String name;
    private String description;
    private final ActiveAppList apps;
    private final TaskQueue tasks;
    private final long createdAt;

    // ---- extra functional metadata ----
    private final List<String> goals;          // remaining goals
    private final List<String> relatedFolders; // paths / folder labels
    private String continueHint;               // where to continue previous work
    private long totalWorkMs;                  // accumulated active time
    private long sessionStartMs;               // when this workspace was last activated (0 if inactive)

    public Workspace(String name, String description) {
        this.name = name;
        this.description = description != null ? description : "";
        this.apps = new ActiveAppList();
        this.tasks = new TaskQueue();
        this.createdAt = System.currentTimeMillis();
        this.goals = new ArrayList<>();
        this.relatedFolders = new ArrayList<>();
        this.continueHint = "Open an app to begin.";
        this.totalWorkMs = 0;
        this.sessionStartMs = 0;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d != null ? d : ""; }
    public ActiveAppList getApps() { return apps; }
    public TaskQueue getTasks() { return tasks; }
    public long getCreatedAt() { return createdAt; }

    public List<String> getGoals() { return goals; }
    public List<String> getRelatedFolders() { return relatedFolders; }
    public String getContinueHint() { return continueHint; }
    public void setContinueHint(String h) { this.continueHint = h != null ? h : ""; }

    public void addGoal(String goal) {
        if (goal != null && !goal.isBlank()) goals.add(goal.trim());
    }

    public boolean removeGoal(String goal) {
        return goals.remove(goal);
    }

    public void addFolder(String folder) {
        if (folder != null && !folder.isBlank() && !relatedFolders.contains(folder.trim())) {
            relatedFolders.add(folder.trim());
        }
    }

    public boolean removeFolder(String folder) {
        return relatedFolders.remove(folder);
    }

    /** Start counting work time for this workspace. */
    public void startSession() {
        if (sessionStartMs == 0) {
            sessionStartMs = System.currentTimeMillis();
        }
    }

    /** Pause counting and add elapsed time to total. */
    public void endSession() {
        if (sessionStartMs > 0) {
            totalWorkMs += System.currentTimeMillis() - sessionStartMs;
            sessionStartMs = 0;
        }
    }

    /** Total work time including current open session. */
    public long getTotalWorkMs() {
        long extra = 0;
        if (sessionStartMs > 0) {
            extra = System.currentTimeMillis() - sessionStartMs;
        }
        return totalWorkMs + extra;
    }

    /** Human-readable work time, e.g. "1h 12m" or "5m". */
    public String getFormattedWorkTime() {
        long ms = getTotalWorkMs();
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public int appCount() { return apps.size(); }
    public int taskCount() { return tasks.size(); }
    public int goalCount() { return goals.size(); }

    @Override
    public String toString() {
        return name + "  (" + appCount() + " apps, " + taskCount() + " tasks, "
                + goalCount() + " goals)";
    }
}

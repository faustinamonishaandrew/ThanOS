import java.util.*;
import java.util.stream.Collectors;

public class WorkspaceManager {
    private final Map<String, Workspace> workspaces = new LinkedHashMap<>();
    private String activeWorkspaceId;
    private final WorkspaceQueue actionQueue = new WorkspaceQueue();
    private int nextId = 1;

    public WorkspaceManager() {
        Workspace main = new Workspace("ws-main", "Main");
        workspaces.put(main.getId(), main);
        activeWorkspaceId = main.getId();
    }

    public Workspace getActiveWorkspace() {
        return workspaces.get(activeWorkspaceId);
    }

    public String getActiveWorkspaceId() {
        return activeWorkspaceId;
    }

    public Collection<Workspace> getAllWorkspaces() {
        return workspaces.values();
    }

    public Workspace getWorkspace(String id) {
        return workspaces.get(id);
    }

    public Workspace createWorkspace(String name) {
        String id = "ws-" + (nextId++);
        Workspace ws = new Workspace(id, name == null || name.isBlank() ? "Workspace " + id : name.trim());
        workspaces.put(id, ws);
        return ws;
    }

    public boolean renameWorkspace(String id, String newName) {
        Workspace ws = workspaces.get(id);
        if (ws == null || newName == null || newName.isBlank()) return false;
        ws.setName(newName.trim());
        return true;
    }

    public boolean deleteWorkspace(String id, WindowManager windowManager) {
        if (workspaces.size() <= 1) return false;
        Workspace ws = workspaces.get(id);
        if (ws == null) return false;
        for (String wid : new ArrayList<>(ws.getWindowIds())) {
            windowManager.removeWindow(wid);
        }
        workspaces.remove(id);
        if (id.equals(activeWorkspaceId)) {
            activeWorkspaceId = workspaces.keySet().iterator().next();
        }
        return true;
    }

    public boolean switchTo(String id) {
        if (!workspaces.containsKey(id)) return false;
        activeWorkspaceId = id;
        return true;
    }

    public void enqueueAction(WorkspaceAction action) {
        actionQueue.enqueue(action);
    }

    public WorkspaceQueue getActionQueue() {
        return actionQueue;
    }

    public List<String> processQueue(WindowManager windowManager, AppManager appManager) {
        List<String> results = new ArrayList<>();
        while (!actionQueue.isEmpty()) {
            WorkspaceAction action = actionQueue.dequeue();
            if (action == null) continue;
            try {
                switch (action.getType()) {
                    case CREATE_WORKSPACE -> {
                        Workspace ws = createWorkspace(action.getPayload());
                        results.add("Created: " + ws.getName());
                    }
                    case SWITCH_WORKSPACE -> {
                        if (switchTo(action.getTargetId()))
                            results.add("Switched to: " + getActiveWorkspace().getName());
                        else results.add("Switch failed");
                    }
                    case RENAME_WORKSPACE -> {
                        if (renameWorkspace(action.getTargetId(), action.getPayload()))
                            results.add("Renamed");
                    }
                    case DELETE_WORKSPACE -> {
                        if (deleteWorkspace(action.getTargetId(), windowManager))
                            results.add("Deleted");
                        else results.add("Cannot delete last workspace");
                    }
                    case OPEN_APP -> results.add("OPEN_APP:" + action.getTargetId() + ":" +
                            (action.getPayload() != null ? action.getPayload() : activeWorkspaceId));
                }
            } catch (Exception e) {
                results.add("Error: " + e.getMessage());
            }
        }
        return results;
    }

    public String toJsonArray() {
        return "[" + workspaces.values().stream().map(Workspace::toJson).collect(Collectors.joining(",")) + "]";
    }

    public void loadWorkspaces(List<Workspace> list, String activeId) {
        workspaces.clear();
        for (Workspace ws : list) workspaces.put(ws.getId(), ws);
        if (workspaces.isEmpty()) {
            Workspace main = new Workspace("ws-main", "Main");
            workspaces.put(main.getId(), main);
            activeWorkspaceId = main.getId();
        } else {
            activeWorkspaceId = workspaces.containsKey(activeId) ? activeId : workspaces.keySet().iterator().next();
        }
        nextId = 1;
        for (String id : workspaces.keySet()) {
            if (id.startsWith("ws-")) {
                try {
                    int n = Integer.parseInt(id.substring(3));
                    if (n >= nextId) nextId = n + 1;
                } catch (NumberFormatException ignored) {}
            }
        }
    }
}

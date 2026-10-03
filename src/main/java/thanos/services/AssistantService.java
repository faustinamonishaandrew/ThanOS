package thanos.services;

/** Offline keyword-based assistant. No network, no model claims. */
public class AssistantService {

    private final WorkspaceService ws;

    public AssistantService(WorkspaceService ws) { this.ws = ws; }

    public String respond(String input) {
        if (input == null || input.isBlank())
            return "Type a question. I am a local offline helper.";
        String q = input.toLowerCase().trim();

        if (has(q, "hello", "hi", "hey"))
            return "Hello. I am the ThanOS Assistant — fully offline. "
                 + "Ask about workspaces, apps, tasks, or data structures.";

        if (has(q, "help", "what can"))
            return "I can help with:\n"
                 + "• Workspaces — create, switch, go back\n"
                 + "• Apps — File Manager, Notes, Calculator, Tasks…\n"
                 + "• System status and data structures\n"
                 + "Type a topic to get started.";

        if (has(q, "workspace", "context", "switch"))
            return "You are in \"" + ws.getActiveName() + "\". "
                 + "Use the Workspace panel to switch, create, rename, or delete. "
                 + "Each workspace keeps its own open apps (Linked List) and tasks (Queue). "
                 + "History is a Stack — use ← Back to return.";

        if (has(q, "file", "folder", "browse"))
            return "Open File Manager to browse local folders, navigate, and create directories.";

        if (has(q, "note", "write"))
            return "Open Notes to create, edit, and save notes locally under thanos_data/notes/.";

        if (has(q, "calc", "math"))
            return "The Calculator handles basic arithmetic.";

        if (has(q, "task", "queue", "process"))
            return "Task Manager shows the Linked List of apps in the current workspace "
                 + "and its FIFO task Queue. Enqueue tasks and Process Next to dequeue.";

        if (has(q, "calendar", "date", "month"))
            return "Open Calendar for a month-view with navigation.";

        if (has(q, "setting", "theme", "dark", "light"))
            return "Open Settings to toggle dark/light theme.";

        if (has(q, "status", "how many")) {
            var active = ws.getActive();
            return "Active workspace: " + ws.getActiveName() + "\n"
                 + "• Open apps (Linked List): " + (active != null ? active.appCount() : 0) + "\n"
                 + "• Pending tasks (Queue): " + (active != null ? active.taskCount() : 0) + "\n"
                 + "• Total workspaces (HashMap): " + ws.getWorkspaceMap().size() + "\n"
                 + "• History depth (Stack): " + ws.getHistory().size();
        }

        if (has(q, "linked list", "hashmap", "hash map", "queue", "stack", "data structure"))
            return "ThanOS data structures in the Dynamic Workspace:\n"
                 + "• HashMap — WorkspaceMap + AppRegistry (fast lookup)\n"
                 + "• Linked List — ActiveAppList per workspace\n"
                 + "• Queue — TaskQueue per workspace (FIFO)\n"
                 + "• Stack — HistoryStack for workspace navigation\n"
                 + "Open an app or switch workspace to see them work together.";

        if (has(q, "about", "thanos", "who"))
            return "ThanOS is a student-built context-aware desktop environment. "
                 + "Dynamic Workspaces are the core feature. Everything runs offline.";

        return "Try asking about workspaces, apps, tasks, status, or data structures. "
             + "Type \"help\" for ideas.";
    }

    private boolean has(String q, String... keys) {
        for (String k : keys) if (q.contains(k)) return true;
        return false;
    }
}

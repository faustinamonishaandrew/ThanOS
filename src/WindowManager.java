import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Open-window tracking with LinkedList.
 */
public class WindowManager {
    private final LinkedList<WindowModel> openWindows = new LinkedList<>();

    public void addWindow(WindowModel window) {
        openWindows.addLast(window);
    }

    public boolean removeWindow(String windowId) {
        return openWindows.removeIf(w -> w.getId().equals(windowId));
    }

    public WindowModel findById(String windowId) {
        for (WindowModel w : openWindows) {
            if (w.getId().equals(windowId)) return w;
        }
        return null;
    }

    public List<WindowModel> getWindowsForWorkspace(String workspaceId) {
        return openWindows.stream()
                .filter(w -> workspaceId.equals(w.getWorkspaceId()))
                .collect(Collectors.toList());
    }

    public List<WindowModel> getAllWindows() {
        return new LinkedList<>(openWindows);
    }

    public LinkedList<WindowModel> getOpenWindowsList() {
        return openWindows;
    }

    public int getOpenCount() {
        return openWindows.size();
    }

    public void focusWindow(String windowId) {
        for (WindowModel w : openWindows) {
            w.setState(w.getId().equals(windowId) ? WindowModel.State.FOCUSED : WindowModel.State.NORMAL);
        }
    }

    public void minimizeWindow(String windowId) {
        WindowModel w = findById(windowId);
        if (w != null) w.setState(WindowModel.State.MINIMIZED);
    }

    public void restoreWindow(String windowId) {
        WindowModel w = findById(windowId);
        if (w != null) {
            w.setState(WindowModel.State.FOCUSED);
            openWindows.remove(w);
            openWindows.addLast(w);
        }
    }

    public String toJsonArray(String workspaceId) {
        List<WindowModel> list = workspaceId == null
                ? getAllWindows()
                : getWindowsForWorkspace(workspaceId);
        return "[" + list.stream().map(WindowModel::toJson).collect(Collectors.joining(",")) + "]";
    }
}

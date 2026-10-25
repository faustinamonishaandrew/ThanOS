import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Workspace {
    private String id;
    private String name;
    private final List<String> windowIds = new ArrayList<>();

    public Workspace(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getWindowIds() { return windowIds; }

    public void addWindowId(String windowId) {
        if (!windowIds.contains(windowId)) windowIds.add(windowId);
    }

    public void removeWindowId(String windowId) {
        windowIds.remove(windowId);
    }

    public String toJson() {
        String ids = windowIds.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
        return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"windowIds\":[%s]}",
                id, escape(name), ids);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

import java.util.UUID;

public class WindowModel {
    public enum State { NORMAL, MINIMIZED, FOCUSED }

    private final String id;
    private final String appId;
    private final String title;
    private String workspaceId;
    private State state;
    private double x, y, width, height;

    public WindowModel(String appId, String title, String workspaceId) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.appId = appId;
        this.title = title;
        this.workspaceId = workspaceId;
        this.state = State.FOCUSED;
        this.x = 80 + Math.random() * 100;
        this.y = 60 + Math.random() * 60;
        this.width = 560;
        this.height = 420;
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getTitle() { return title; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"appId\":\"%s\",\"title\":\"%s\",\"workspaceId\":\"%s\"," +
            "\"state\":\"%s\",\"x\":%.0f,\"y\":%.0f,\"width\":%.0f,\"height\":%.0f}",
            id, appId, escape(title), workspaceId, state.name(), x, y, width, height
        );
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

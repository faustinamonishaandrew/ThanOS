package thanos.model;

/**
 * One running application instance inside a workspace.
 * Stored in the workspace's ActiveAppList (Linked List).
 */
public class RunningApp {
    private final String instanceId;   // unique per open window
    private final String appId;        // references AppInfo.id
    private final String appName;
    private final String glyph;
    private AppState state;
    private double winX, winY, winW, winH;
    private boolean maximized;

    public enum AppState { RUNNING, MINIMIZED, FOCUSED }

    public RunningApp(String instanceId, String appId, String appName, String glyph) {
        this.instanceId = instanceId;
        this.appId = appId;
        this.appName = appName;
        this.glyph = glyph;
        this.state = AppState.FOCUSED;
        this.winX = 100;
        this.winY = 60;
        this.winW = 560;
        this.winH = 400;
        this.maximized = false;
    }

    public String getInstanceId() { return instanceId; }
    public String getAppId() { return appId; }
    public String getAppName() { return appName; }
    public String getGlyph() { return glyph; }
    public AppState getState() { return state; }
    public void setState(AppState s) { this.state = s; }

    public double getWinX() { return winX; }
    public double getWinY() { return winY; }
    public double getWinW() { return winW; }
    public double getWinH() { return winH; }
    public boolean isMaximized() { return maximized; }

    public void setGeometry(double x, double y, double w, double h, boolean max) {
        this.winX = x; this.winY = y; this.winW = w; this.winH = h; this.maximized = max;
    }

    @Override
    public String toString() {
        return glyph + "  " + appName + "  [" + state + "]";
    }
}

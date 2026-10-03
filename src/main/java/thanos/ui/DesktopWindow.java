package thanos.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/** Movable / minimizable / maximizable desktop window bound to a RunningApp instance. */
public class DesktopWindow extends VBox {

    private final String instanceId;
    private final String workspaceName;   // which workspace owns this window
    private final Pane desktop;
    private final Runnable onClose, onMinimize, onFocus;

    private double dragOX, dragOY;
    private boolean maximized = false;
    private double rX, rY, rW, rH;
    private final StackPane contentArea;

    public DesktopWindow(String instanceId, String title, String workspaceName,
                         Pane desktop, Runnable onClose, Runnable onMinimize, Runnable onFocus) {
        this.instanceId = instanceId;
        this.workspaceName = workspaceName;
        this.desktop = desktop;
        this.onClose = onClose;
        this.onMinimize = onMinimize;
        this.onFocus = onFocus;

        getStyleClass().add("win");
        setPrefSize(560, 400);
        setMinSize(300, 180);

        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(5, 8, 5, 10));
        bar.getStyleClass().add("win-bar");

        Label t = new Label(title);
        t.getStyleClass().add("win-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button min = wBtn("─");
        min.setOnAction(e -> { setVisible(false); if (onMinimize != null) onMinimize.run(); });
        Button max = wBtn("□");
        max.setOnAction(e -> toggleMax());
        Button cls = wBtn("✕");
        cls.getStyleClass().add("win-close");
        cls.setOnAction(e -> {
            desktop.getChildren().remove(this);
            if (onClose != null) onClose.run();
        });
        bar.getChildren().addAll(t, sp, min, max, cls);

        bar.setOnMousePressed(e -> {
            dragOX = e.getSceneX() - getLayoutX();
            dragOY = e.getSceneY() - getLayoutY();
            toFront();
            if (onFocus != null) onFocus.run();
        });
        bar.setOnMouseDragged(e -> {
            if (maximized) return;
            double nx = Math.max(0, Math.min(e.getSceneX() - dragOX, desktop.getWidth() - 60));
            double ny = Math.max(0, Math.min(e.getSceneY() - dragOY, desktop.getHeight() - 30));
            setLayoutX(nx); setLayoutY(ny);
        });

        contentArea = new StackPane();
        contentArea.getStyleClass().add("win-body");
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        getChildren().addAll(bar, contentArea);

        setOnMousePressed(e -> { toFront(); if (onFocus != null) onFocus.run(); });
    }

    public void setContent(Region c) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(c);
    }

    public String getInstanceId() { return instanceId; }
    public String getWorkspaceName() { return workspaceName; }

    public void restore() {
        setVisible(true);
        toFront();
        if (onFocus != null) onFocus.run();
    }

    public void saveGeometry() {
        // geometry is read by caller if needed
    }

    private void toggleMax() {
        if (!maximized) {
            rX = getLayoutX(); rY = getLayoutY(); rW = getWidth(); rH = getHeight();
            setLayoutX(0); setLayoutY(0);
            setPrefSize(desktop.getWidth(), desktop.getHeight());
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            maximized = true;
        } else {
            setLayoutX(rX); setLayoutY(rY);
            setPrefSize(rW, rH);
            setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            maximized = false;
        }
    }

    private Button wBtn(String t) {
        Button b = new Button(t);
        b.getStyleClass().add("win-btn");
        b.setFocusTraversable(false);
        return b;
    }
}

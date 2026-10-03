package thanos.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import thanos.apps.*;
import thanos.model.*;
import thanos.services.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Full desktop environment with genuine Dynamic Workspace support.
 *
 * Key behaviour:
 *  - Each workspace owns its RunningApps (Linked List) and tasks (Queue).
 *  - Windows are tagged with workspaceName.
 *  - On switch: hide windows of old workspace, show windows of new one.
 *  - State is preserved — switching back restores previous windows.
 */
public class DesktopController {

    private final Stage stage;
    private final WorkspaceService ws = new WorkspaceService();
    private final PersistenceService store = new PersistenceService();
    private final AssistantService ai = new AssistantService(ws);

    private Pane desktopArea;
    private HBox taskbarApps;
    private Label clockLabel, notifLabel, wsIndicator, batteryLabel, wifiLabel, volLabel;
    private VBox wsPanel, startMenu;
    private boolean startOpen = false;
    private Scene scene;
    private boolean dark = true;

    // instanceId → DesktopWindow  (all workspaces; visibility controlled by workspace)
    private final Map<String, DesktopWindow> allWindows = new LinkedHashMap<>();

    // simple mock system values
    private int batteryPct = 78;
    private boolean wifiOn = true;

    public DesktopController(Stage stage) {
        this.stage = stage;
        ws.setNotifyHandler(this::toast);
        ws.setWorkspaceChangedHandler(this::onWorkspaceSwitch);
    }

    // ================================================================
    // PUBLIC ENTRY
    // ================================================================

    /** Called after successful login. */
    public BorderPane buildDesktop() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("desk-root");

        StackPane center = new StackPane();
        desktopArea = new Pane();
        desktopArea.getStyleClass().add("desk-area");

        // desktop icons
        VBox icons = buildIcons();
        icons.setLayoutX(18); icons.setLayoutY(18);
        desktopArea.getChildren().add(icons);

        // widgets (top-right area of desktop)
        VBox widgets = buildWidgets();
        widgets.setLayoutX(0); // positioned after scene knows width
        desktopArea.getChildren().add(widgets);
        desktopArea.widthProperty().addListener((o, a, w) ->
                widgets.setLayoutX(Math.max(0, w.doubleValue() - 230)));
        widgets.setLayoutY(16);

        startMenu = buildStartMenu();
        startMenu.setVisible(false);
        startMenu.setManaged(false);
        StackPane.setAlignment(startMenu, Pos.BOTTOM_LEFT);
        StackPane.setMargin(startMenu, new Insets(0, 0, 50, 6));

        center.getChildren().addAll(desktopArea, startMenu);
        root.setCenter(center);

        wsPanel = buildWorkspacePanel();
        root.setRight(wsPanel);
        root.setBottom(buildTaskbar());

        String theme = store.loadSetting("theme", "dark");
        dark = !"light".equals(theme);
        startClock();
        return root;
    }

    public void setScene(Scene s) {
        scene = s;
        applyTheme(dark);
    }

    // ================================================================
    // WORKSPACE SWITCH — the core of Dynamic Workspace
    // ================================================================

    /**
     * Called by WorkspaceService when active workspace changes.
     * Hides all windows not belonging to the new workspace,
     * shows (restores visibility of) windows that do.
     */
    private void onWorkspaceSwitch(String newName) {
        for (DesktopWindow w : allWindows.values()) {
            boolean belongs = w.getWorkspaceName().equalsIgnoreCase(newName);
            // only show if it belongs AND was not user-minimized
            // (we use isVisible as minimize flag; restore if belongs)
            if (belongs) {
                // find matching RunningApp state
                RunningApp ra = null;
                for (RunningApp a : ws.getActiveApps()) {
                    if (a.getInstanceId().equals(w.getInstanceId())) { ra = a; break; }
                }
                if (ra != null && ra.getState() != RunningApp.AppState.MINIMIZED) {
                    w.setVisible(true);
                } else if (ra != null) {
                    w.setVisible(false);
                } else {
                    w.setVisible(true); // fallback
                }
            } else {
                w.setVisible(false);
            }
        }
        rebuildTaskbar();
        refreshWsPanel();
        if (wsIndicator != null) wsIndicator.setText("◈  " + newName);
    }

    // ================================================================
    // ICONS
    // ================================================================
    private VBox buildIcons() {
        VBox box = new VBox(14);
        for (AppInfo info : ws.getAppRegistry().all()) {
            VBox icon = new VBox(3);
            icon.setAlignment(Pos.CENTER);
            icon.setPrefWidth(70);
            icon.getStyleClass().add("desk-icon");
            icon.setOnMouseClicked(e -> { if (e.getClickCount() == 2) openApp(info.getId()); });
            Label g = new Label(info.getGlyph());
            g.getStyleClass().add("icon-glyph");
            Label n = new Label(info.getName());
            n.getStyleClass().add("icon-name");
            n.setWrapText(true); n.setMaxWidth(70); n.setAlignment(Pos.CENTER);
            icon.getChildren().addAll(g, n);
            box.getChildren().add(icon);
        }
        return box;
    }

    // ================================================================
    // WIDGETS (calendar mini, weather, system status)
    // ================================================================
    private VBox buildWidgets() {
        VBox box = new VBox(10);
        box.setPrefWidth(210);
        box.getStyleClass().add("widget-stack");

        // Clock widget
        VBox clockW = new VBox(2);
        clockW.getStyleClass().add("widget");
        clockW.setPadding(new Insets(10));
        Label cwTime = new Label();
        cwTime.getStyleClass().add("widget-time");
        Label cwDate = new Label();
        cwDate.getStyleClass().add("widget-date");
        clockW.getChildren().addAll(cwTime, cwDate);
        // update with main clock
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            cwTime.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));
            cwDate.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMM d")));
        }));
        t.setCycleCount(Timeline.INDEFINITE); t.play();

        // Weather (mock / offline)
        VBox weatherW = new VBox(2);
        weatherW.getStyleClass().add("widget");
        weatherW.setPadding(new Insets(10));
        Label wTitle = new Label("Weather");
        wTitle.getStyleClass().add("widget-title");
        Label wVal = new Label("⛅  28°C  Partly Cloudy");
        wVal.getStyleClass().add("widget-body");
        Label wNote = new Label("Local mock · offline");
        wNote.getStyleClass().add("hint");
        weatherW.getChildren().addAll(wTitle, wVal, wNote);

        // System status
        VBox sysW = new VBox(2);
        sysW.getStyleClass().add("widget");
        sysW.setPadding(new Insets(10));
        Label sTitle = new Label("System");
        sTitle.getStyleClass().add("widget-title");
        Label sBody = new Label("Battery " + batteryPct + "%  ·  Wi-Fi On  ·  Vol 60%");
        sBody.getStyleClass().add("widget-body");
        sysW.getChildren().addAll(sTitle, sBody);

        box.getChildren().addAll(clockW, weatherW, sysW);
        return box;
    }

    // ================================================================
    // START MENU
    // ================================================================
    private VBox buildStartMenu() {
        VBox menu = new VBox(3);
        menu.setPadding(new Insets(10));
        menu.setPrefWidth(250);
        menu.getStyleClass().add("start-menu");

        Label hdr = new Label("ThanOS");
        hdr.getStyleClass().add("start-hdr");
        TextField search = new TextField();
        search.setPromptText("Search apps…");
        search.getStyleClass().add("start-search");

        menu.getChildren().addAll(hdr, search, new Separator());

        List<Button> items = new ArrayList<>();
        for (AppInfo info : ws.getAppRegistry().all()) {
            Button b = new Button(info.getGlyph() + "   " + info.getName());
            b.getStyleClass().add("start-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setOnAction(e -> { openApp(info.getId()); toggleStart(); });
            items.add(b);
            menu.getChildren().add(b);
        }
        search.textProperty().addListener((o, a, q) -> {
            String qq = q == null ? "" : q.toLowerCase();
            for (Button b : items) {
                boolean show = qq.isEmpty() || b.getText().toLowerCase().contains(qq);
                b.setVisible(show); b.setManaged(show);
            }
        });
        return menu;
    }

    private void toggleStart() {
        startOpen = !startOpen;
        startMenu.setVisible(startOpen);
        startMenu.setManaged(startOpen);
        if (startOpen) startMenu.toFront();
    }

    // ================================================================
    // WORKSPACE PANEL (right side) — create / rename / delete / switch
    // ================================================================
    private VBox buildWorkspacePanel() {
        VBox panel = new VBox(6);
        panel.setPadding(new Insets(10, 8, 10, 8));
        panel.setPrefWidth(240);
        panel.setMinWidth(220);
        panel.getStyleClass().add("ws-panel");

        Label hdr = new Label("Workspaces");
        hdr.getStyleClass().add("panel-hdr");

        Label active = new Label(ws.getActiveName());
        active.setId("ws-active-label");
        active.getStyleClass().add("ws-active");

        // ---- detail card for active workspace ----
        VBox detail = new VBox(4);
        detail.setId("ws-detail");
        detail.getStyleClass().add("ws-detail");
        detail.setPadding(new Insets(8));
        fillWsDetail(detail);

        VBox list = new VBox(3);
        list.setId("ws-list");
        fillWsList(list);

        Button back = new Button("← Previous");
        back.getStyleClass().add("btn");
        back.setMaxWidth(Double.MAX_VALUE);
        back.setOnAction(e -> ws.goBack());

        Button create = new Button("+ New Workspace");
        create.getStyleClass().add("btn-accent");
        create.setMaxWidth(Double.MAX_VALUE);
        create.setOnAction(e -> createWorkspaceDialog());

        Button addGoal = new Button("+ Goal");
        addGoal.getStyleClass().add("btn");
        addGoal.setMaxWidth(Double.MAX_VALUE);
        addGoal.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Add Goal"); d.setHeaderText(null); d.setContentText("Goal:");
            d.showAndWait().ifPresent(g -> {
                ws.addGoalToActive(g.trim());
                refreshWsPanel();
            });
        });

        Button addFolder = new Button("+ Folder");
        addFolder.getStyleClass().add("btn");
        addFolder.setMaxWidth(Double.MAX_VALUE);
        addFolder.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog("~/Documents");
            d.setTitle("Link Folder"); d.setHeaderText(null); d.setContentText("Folder path/label:");
            d.showAndWait().ifPresent(f -> {
                ws.addFolderToActive(f.trim());
                refreshWsPanel();
            });
        });

        Button rename = new Button("Rename");
        rename.getStyleClass().add("btn");
        rename.setMaxWidth(Double.MAX_VALUE);
        rename.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog(ws.getActiveName());
            d.setTitle("Rename"); d.setHeaderText(null); d.setContentText("New name:");
            d.showAndWait().ifPresent(n -> {
                if (ws.renameWorkspace(ws.getActiveName(), n.trim())) refreshWsPanel();
            });
        });

        Button delete = new Button("Delete");
        delete.getStyleClass().add("btn-danger");
        delete.setMaxWidth(Double.MAX_VALUE);
        delete.setOnAction(e -> {
            List<String> names = ws.getWorkspaceMap().names();
            names.removeIf(n -> n.equalsIgnoreCase(ws.getActiveName()));
            if (names.isEmpty()) { toast("No other workspace to delete"); return; }
            ChoiceDialog<String> cd = new ChoiceDialog<>(names.get(0), names);
            cd.setTitle("Delete Workspace"); cd.setHeaderText(null); cd.setContentText("Delete:");
            cd.showAndWait().ifPresent(n -> {
                allWindows.entrySet().removeIf(en -> {
                    if (en.getValue().getWorkspaceName().equalsIgnoreCase(n)) {
                        desktopArea.getChildren().remove(en.getValue());
                        return true;
                    }
                    return false;
                });
                ws.deleteWorkspace(n);
                refreshWsPanel();
            });
        });

        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);
        Label ds = new Label("HashMap · LinkedList\nQueue · Stack");
        ds.getStyleClass().add("ds-foot");

        panel.getChildren().addAll(hdr, active, detail, list,
                back, create, addGoal, addFolder, rename, delete, sp, ds);
        return panel;
    }

    /** Fill the detail card: description, continue-from, work time, folders, goals. */
    private void fillWsDetail(VBox detail) {
        detail.getChildren().clear();
        Workspace w = ws.getActive();
        if (w == null) return;

        Label desc = new Label(w.getDescription().isBlank() ? "(no description)" : w.getDescription());
        desc.getStyleClass().add("ws-desc");
        desc.setWrapText(true);

        Label contTitle = new Label("Continue from");
        contTitle.getStyleClass().add("ws-section");
        Label cont = new Label(w.getContinueHint());
        cont.getStyleClass().add("ws-body");
        cont.setWrapText(true);

        Label timeTitle = new Label("Work time");
        timeTitle.getStyleClass().add("ws-section");
        Label time = new Label(w.getFormattedWorkTime());
        time.setId("ws-work-time");
        time.getStyleClass().add("ws-time");

        Label foldTitle = new Label("Related folders");
        foldTitle.getStyleClass().add("ws-section");
        VBox folders = new VBox(2);
        if (w.getRelatedFolders().isEmpty()) {
            Label empty = new Label("  (none linked)");
            empty.getStyleClass().add("hint");
            folders.getChildren().add(empty);
        } else {
            for (String f : w.getRelatedFolders()) {
                Label fl = new Label("📁  " + f);
                fl.getStyleClass().add("ws-body");
                fl.setWrapText(true);
                folders.getChildren().add(fl);
            }
        }

        Label goalTitle = new Label("Goals remaining  (" + w.goalCount() + ")");
        goalTitle.getStyleClass().add("ws-section");
        VBox goals = new VBox(2);
        goals.setId("ws-goals");
        if (w.getGoals().isEmpty()) {
            Label empty = new Label("  (no goals)");
            empty.getStyleClass().add("hint");
            goals.getChildren().add(empty);
        } else {
            for (String g : w.getGoals()) {
                HBox row = new HBox(4);
                row.setAlignment(Pos.CENTER_LEFT);
                Label gl = new Label("○  " + g);
                gl.getStyleClass().add("ws-body");
                gl.setWrapText(true);
                HBox.setHgrow(gl, Priority.ALWAYS);
                Button done = new Button("✓");
                done.getStyleClass().add("btn-tiny");
                final String goalText = g;
                done.setOnAction(e -> {
                    ws.completeGoal(goalText);
                    refreshWsPanel();
                });
                row.getChildren().addAll(gl, done);
                goals.getChildren().add(row);
            }
        }

        Label appsInfo = new Label("Open apps: " + w.appCount() + "  ·  Tasks: " + w.taskCount());
        appsInfo.getStyleClass().add("hint");

        detail.getChildren().addAll(desc, contTitle, cont, timeTitle, time,
                foldTitle, folders, goalTitle, goals, appsInfo);
    }

    private void createWorkspaceDialog() {
        Dialog<Workspace> dialog = new Dialog<>();
        dialog.setTitle("New Workspace");
        dialog.setHeaderText("Create a Dynamic Workspace");

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        grid.setPadding(new Insets(10));
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField descField = new TextField();
        descField.setPromptText("Description / goal summary");
        TextField folderField = new TextField();
        folderField.setPromptText("Related folder (optional)");
        TextField goalField = new TextField();
        goalField.setPromptText("First goal (optional)");
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Description"), descField);
        grid.addRow(2, new Label("Folder"), folderField);
        grid.addRow(3, new Label("Goal"), goalField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String name = nameField.getText().trim();
            if (name.isEmpty()) return null;
            Workspace created = ws.createWorkspace(name, descField.getText().trim());
            if (created != null) {
                if (!folderField.getText().isBlank()) created.addFolder(folderField.getText().trim());
                if (!goalField.getText().isBlank()) created.addGoal(goalField.getText().trim());
                created.setContinueHint("New workspace — open an app to begin.");
            }
            return created;
        });

        dialog.showAndWait().ifPresent(w -> refreshWsPanel());
    }

    private void fillWsList(VBox list) {
        list.getChildren().clear();
        for (Workspace w : ws.getWorkspaceMap().all()) {
            Button b = new Button(w.getName() + "  (" + w.appCount() + ")");
            b.setMaxWidth(Double.MAX_VALUE);
            b.getStyleClass().add("ws-btn");
            if (w.getName().equalsIgnoreCase(ws.getActiveName()))
                b.getStyleClass().add("ws-btn-on");
            final String name = w.getName();
            b.setOnAction(e -> ws.switchWorkspace(name));
            list.getChildren().add(b);
        }
    }

    private void refreshWsPanel() {
        if (wsPanel == null) return;
        Label active = (Label) wsPanel.lookup("#ws-active-label");
        if (active != null) active.setText(ws.getActiveName());
        VBox list = (VBox) wsPanel.lookup("#ws-list");
        if (list != null) fillWsList(list);
        VBox detail = (VBox) wsPanel.lookup("#ws-detail");
        if (detail != null) fillWsDetail(detail);
    }

    // ================================================================
    // TASKBAR
    // ================================================================
    private HBox buildTaskbar() {
        HBox bar = new HBox(5);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(3, 8, 3, 8));
        bar.setPrefHeight(46);
        bar.getStyleClass().add("taskbar");

        Button start = new Button("◈");
        start.getStyleClass().add("start-btn");
        start.setOnAction(e -> toggleStart());

        taskbarApps = new HBox(3);
        taskbarApps.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(taskbarApps, Priority.ALWAYS);

        wsIndicator = new Label("◈  " + ws.getActiveName());
        wsIndicator.getStyleClass().add("ws-indicator");

        notifLabel = new Label("");
        notifLabel.getStyleClass().add("notif");

        // system tray indicators
        batteryLabel = new Label("🔋 " + batteryPct + "%");
        batteryLabel.getStyleClass().add("tray");
        wifiLabel = new Label("📶");
        wifiLabel.getStyleClass().add("tray");
        volLabel = new Label("🔊");
        volLabel.getStyleClass().add("tray");

        clockLabel = new Label();
        clockLabel.getStyleClass().add("clock");
        clockLabel.setMinWidth(120);
        clockLabel.setAlignment(Pos.CENTER_RIGHT);

        bar.getChildren().addAll(start, taskbarApps, wsIndicator, notifLabel,
                batteryLabel, wifiLabel, volLabel, clockLabel);
        return bar;
    }

    private void rebuildTaskbar() {
        taskbarApps.getChildren().clear();
        // only show apps belonging to the ACTIVE workspace
        for (RunningApp a : ws.getActiveApps()) {
            Button b = new Button(a.getGlyph() + " " + a.getAppName());
            b.getStyleClass().add("tb-app");
            if (a.getState() == RunningApp.AppState.FOCUSED)
                b.getStyleClass().add("tb-app-on");
            final String id = a.getInstanceId();
            b.setOnAction(e -> {
                DesktopWindow w = allWindows.get(id);
                if (w != null) {
                    if (!w.isVisible()) {
                        ws.restoreApp(id);
                        w.restore();
                    } else {
                        w.toFront();
                        ws.focusApp(id);
                    }
                    rebuildTaskbar();
                }
            });
            taskbarApps.getChildren().add(b);
        }
    }

    // ================================================================
    // OPEN APPLICATION
    // ================================================================
    private void openApp(String appId) {
        RunningApp app = ws.openApp(appId);
        if (app == null) return;

        String wsName = ws.getActiveName();
        DesktopWindow window = new DesktopWindow(
                app.getInstanceId(),
                app.getGlyph() + "  " + app.getAppName(),
                wsName,
                desktopArea,
                () -> { // close
                    ws.closeApp(app.getInstanceId());
                    allWindows.remove(app.getInstanceId());
                    rebuildTaskbar();
                    refreshWsPanel();
                },
                () -> { // minimize
                    ws.minimizeApp(app.getInstanceId());
                    rebuildTaskbar();
                },
                () -> { // focus
                    ws.focusApp(app.getInstanceId());
                    rebuildTaskbar();
                }
        );

        window.setContent(createContent(appId));
        int off = (int) allWindows.values().stream()
                .filter(w -> w.getWorkspaceName().equalsIgnoreCase(wsName)).count() * 26;
        window.setLayoutX(90 + off);
        window.setLayoutY(36 + off);

        desktopArea.getChildren().add(window);
        allWindows.put(app.getInstanceId(), window);
        rebuildTaskbar();
        refreshWsPanel();
    }

    private Region createContent(String appId) {
        return switch (appId) {
            case "files"     -> new FileManagerApp().build();
            case "notes"     -> new NotesApp(store).build();
            case "calc"      -> new CalculatorApp().build();
            case "tasks"     -> new TaskManagerApp(ws).build();
            case "calendar"  -> new CalendarApp().build();
            case "settings"  -> new SettingsApp(store, this::applyTheme).build();
            case "assistant" -> new AssistantApp(ai).build();
            default -> new Label("Unknown");
        };
    }

    // ================================================================
    // THEME / CLOCK / TOAST
    // ================================================================
    private void applyTheme(boolean isDark) {
        dark = isDark;
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = isDark ? "/css/dark.css" : "/css/light.css";
        var url = getClass().getResource(css);
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
    }

    private void startClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM  HH:mm");
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalDateTime.now().format(fmt))));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
        clockLabel.setText(LocalDateTime.now().format(fmt));
    }

    private void toast(String msg) {
        notifLabel.setText(msg);
        Timeline c = new Timeline(new KeyFrame(Duration.seconds(3.5), e -> notifLabel.setText("")));
        c.play();
    }
}

package thanos.apps;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import thanos.model.RunningApp;
import thanos.model.WorkspaceTask;
import thanos.services.WorkspaceService;

/**
 * Shows Linked List (active apps in current workspace)
 * and Queue (pending tasks for current workspace).
 */
public class TaskManagerApp {
    private final WorkspaceService ws;
    private final ListView<String> appView = new ListView<>();
    private final ListView<String> taskView = new ListView<>();
    private final Label stats = new Label();

    public TaskManagerApp(WorkspaceService ws) { this.ws = ws; }

    public Region build() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.getStyleClass().add("app-root");

        Label title = new Label("Task Manager");
        title.getStyleClass().add("section-title");
        stats.getStyleClass().add("hint");

        HBox cols = new HBox(14);
        VBox.setVgrow(cols, Priority.ALWAYS);

        VBox left = new VBox(6);
        HBox.setHgrow(left, Priority.ALWAYS);
        Label lt = new Label("Active Apps  (Linked List)");
        lt.getStyleClass().add("sub-title");
        appView.getStyleClass().add("app-list");
        VBox.setVgrow(appView, Priority.ALWAYS);
        Button ref = new Button("Refresh");
        ref.getStyleClass().add("btn");
        ref.setOnAction(e -> refresh());
        left.getChildren().addAll(lt, appView, ref);

        VBox right = new VBox(6);
        HBox.setHgrow(right, Priority.ALWAYS);
        Label rt = new Label("Pending Tasks  (FIFO Queue)");
        rt.getStyleClass().add("sub-title");
        taskView.getStyleClass().add("app-list");
        VBox.setVgrow(taskView, Priority.ALWAYS);
        HBox row = new HBox(6);
        TextField input = new TextField();
        input.setPromptText("New task…");
        input.getStyleClass().add("field");
        HBox.setHgrow(input, Priority.ALWAYS);
        Button enq = new Button("Enqueue");
        enq.getStyleClass().add("btn-accent");
        enq.setOnAction(e -> {
            if (!input.getText().isBlank()) {
                ws.enqueueTask(input.getText().trim());
                input.clear(); refresh();
            }
        });
        Button proc = new Button("Process Next");
        proc.getStyleClass().add("btn");
        proc.setOnAction(e -> { ws.processNextTask(); refresh(); });
        row.getChildren().addAll(input, enq, proc);
        right.getChildren().addAll(rt, taskView, row);

        cols.getChildren().addAll(left, right);

        Label explain = new Label(
            "Each workspace has its own Linked List of apps and Queue of tasks.\n" +
            "Switch workspace → these lists change to that workspace's state.");
        explain.getStyleClass().add("hint");

        root.getChildren().addAll(title, stats, cols, explain);
        refresh();
        return root;
    }

    private void refresh() {
        appView.getItems().clear();
        for (RunningApp a : ws.getActiveApps())
            appView.getItems().add(a.toString() + "  id=" + a.getInstanceId());
        if (appView.getItems().isEmpty()) appView.getItems().add("(no open apps in this workspace)");

        taskView.getItems().clear();
        for (WorkspaceTask t : ws.getPendingTasks())
            taskView.getItems().add(t.toString());
        if (taskView.getItems().isEmpty()) taskView.getItems().add("(queue empty)");

        var active = ws.getActive();
        stats.setText("Workspace: " + ws.getActiveName()
            + "  |  Apps: " + (active != null ? active.appCount() : 0)
            + "  |  Tasks: " + (active != null ? active.taskCount() : 0));
    }
}

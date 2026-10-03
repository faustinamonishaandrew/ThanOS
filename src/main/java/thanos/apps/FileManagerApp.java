package thanos.apps;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import java.nio.file.*;
import java.util.*;

public class FileManagerApp {
    private Path current = Paths.get(System.getProperty("user.home"));
    private final ListView<String> list = new ListView<>();
    private final Label pathLbl = new Label();
    private final Deque<Path> back = new ArrayDeque<>();
    private final Deque<Path> fwd = new ArrayDeque<>();

    public Region build() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("app-root");

        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        Button bBack = mkBtn("←", e -> goBack());
        Button bFwd  = mkBtn("→", e -> goFwd());
        Button bUp   = mkBtn("↑", e -> goUp());
        Button bRef  = mkBtn("↻", e -> refresh());
        Button bNew  = mkBtn("+ Folder", e -> newFolder());
        bNew.getStyleClass().add("btn-accent");
        bar.getChildren().addAll(bBack, bFwd, bUp, bRef, bNew);

        pathLbl.getStyleClass().add("path-label");
        list.getStyleClass().add("app-list");
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) openSel();
        });

        root.getChildren().addAll(bar, pathLbl, list);
        refresh();
        return root;
    }

    private Button mkBtn(String t, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(t);
        b.getStyleClass().add("btn");
        b.setOnAction(h);
        return b;
    }

    private void refresh() {
        pathLbl.setText(current.toString());
        list.getItems().clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(current)) {
            List<String> dirs = new ArrayList<>(), files = new ArrayList<>();
            for (Path p : ds) {
                String n = p.getFileName().toString();
                if (Files.isDirectory(p)) dirs.add("📁  " + n);
                else files.add("📄  " + n);
            }
            Collections.sort(dirs, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
            list.getItems().addAll(dirs);
            list.getItems().addAll(files);
        } catch (Exception ex) {
            list.getItems().add("(cannot read: " + ex.getMessage() + ")");
        }
    }

    private void openSel() {
        String s = list.getSelectionModel().getSelectedItem();
        if (s == null) return;
        String name = s.replace("📁  ", "").replace("📄  ", "");
        Path next = current.resolve(name);
        if (Files.isDirectory(next)) {
            back.push(current); fwd.clear();
            current = next; refresh();
        }
    }

    private void goBack() {
        if (back.isEmpty()) return;
        fwd.push(current); current = back.pop(); refresh();
    }
    private void goFwd() {
        if (fwd.isEmpty()) return;
        back.push(current); current = fwd.pop(); refresh();
    }
    private void goUp() {
        Path p = current.getParent();
        if (p != null) { back.push(current); fwd.clear(); current = p; refresh(); }
    }
    private void newFolder() {
        TextInputDialog d = new TextInputDialog("New Folder");
        d.setHeaderText(null); d.setContentText("Name:");
        d.showAndWait().ifPresent(n -> {
            if (n.isBlank()) return;
            try { Files.createDirectory(current.resolve(n.trim())); refresh(); }
            catch (Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
        });
    }
}

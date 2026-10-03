package thanos.apps;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import thanos.model.Note;
import thanos.services.PersistenceService;
import java.util.UUID;

public class NotesApp {
    private final PersistenceService store;
    private final ListView<Note> list = new ListView<>();
    private final TextField title = new TextField();
    private final TextArea body = new TextArea();
    private Note current;

    public NotesApp(PersistenceService store) { this.store = store; }

    public Region build() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.getStyleClass().add("app-root");

        VBox left = new VBox(8);
        left.setPrefWidth(170);
        Label h = new Label("Notes");
        h.getStyleClass().add("section-title");
        list.getStyleClass().add("app-list");
        VBox.setVgrow(list, Priority.ALWAYS);
        list.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> { if (n != null) load(n); });
        Button btnNew = new Button("+ New");
        btnNew.getStyleClass().add("btn-accent");
        btnNew.setMaxWidth(Double.MAX_VALUE);
        btnNew.setOnAction(e -> create());
        left.getChildren().addAll(h, list, btnNew);

        VBox right = new VBox(8);
        title.setPromptText("Title");
        title.getStyleClass().add("field");
        body.setPromptText("Write…");
        body.getStyleClass().add("note-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox acts = new HBox(8);
        Button save = new Button("Save");
        save.getStyleClass().add("btn-accent");
        save.setOnAction(e -> save());
        Button del = new Button("Delete");
        del.getStyleClass().add("btn-danger");
        del.setOnAction(e -> delete());
        acts.getChildren().addAll(save, del);
        right.getChildren().addAll(title, body, acts);

        root.setLeft(left);
        root.setCenter(right);
        BorderPane.setMargin(left, new Insets(0, 10, 0, 0));
        reload();
        return root;
    }

    private void reload() { list.getItems().setAll(store.loadNotes()); }

    private void create() {
        Note n = new Note(UUID.randomUUID().toString().substring(0, 8), "Untitled", "");
        store.saveNote(n); reload();
        list.getSelectionModel().select(n); load(n);
    }

    private void load(Note n) {
        current = n;
        title.setText(n.getTitle());
        body.setText(n.getContent());
    }

    private void save() {
        if (current == null) { create(); return; }
        current.setTitle(title.getText().isBlank() ? "Untitled" : title.getText());
        current.setContent(body.getText());
        store.saveNote(current); reload();
    }

    private void delete() {
        if (current == null) return;
        store.deleteNote(current.getId());
        current = null; title.clear(); body.clear(); reload();
    }
}

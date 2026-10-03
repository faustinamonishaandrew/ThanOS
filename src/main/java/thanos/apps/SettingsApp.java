package thanos.apps;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import thanos.services.PersistenceService;
import java.util.function.Consumer;

public class SettingsApp {
    private final PersistenceService store;
    private final Consumer<Boolean> onTheme; // true = dark

    public SettingsApp(PersistenceService store, Consumer<Boolean> onTheme) {
        this.store = store;
        this.onTheme = onTheme;
    }

    public Region build() {
        VBox root = new VBox(14);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("app-root");

        Label title = new Label("Settings");
        title.getStyleClass().add("section-title");

        Label themeL = new Label("Appearance");
        themeL.getStyleClass().add("sub-title");

        ToggleGroup g = new ToggleGroup();
        RadioButton dark = new RadioButton("Dark theme");
        dark.getStyleClass().add("radio");
        dark.setToggleGroup(g);
        RadioButton light = new RadioButton("Light theme");
        light.getStyleClass().add("radio");
        light.setToggleGroup(g);

        String saved = store.loadSetting("theme", "dark");
        if ("light".equals(saved)) light.setSelected(true); else dark.setSelected(true);

        g.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == dark) { store.saveSetting("theme", "dark"); if (onTheme != null) onTheme.accept(true); }
            else if (n == light) { store.saveSetting("theme", "light"); if (onTheme != null) onTheme.accept(false); }
        });

        Label aboutL = new Label("About");
        aboutL.getStyleClass().add("sub-title");
        Label about = new Label(
            "ThanOS v2.0 — Dynamic Workspace Desktop Environment\n" +
            "Data Structures: Linked List · HashMap · Queue · Stack\n" +
            "Fully offline · No cloud · No API keys");
        about.getStyleClass().add("hint");
        about.setWrapText(true);

        root.getChildren().addAll(title, themeL, dark, light, aboutL, about);
        return root;
    }
}

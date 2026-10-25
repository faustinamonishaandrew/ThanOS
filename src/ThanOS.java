import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ThanOS – Desktop environment
 * Java (logic + data structures) + HTML/CSS/JS (UI).
 */
public class ThanOS extends Application {

    private WebView webView;
    private WebEngine engine;
    private JavaBridge bridge;
    private SystemStore store;

    @Override
    public void start(Stage stage) {
        store = new SystemStore();
        store.loadAll();

        webView = new WebView();
        engine = webView.getEngine();
        bridge = new JavaBridge(store, engine);

        // Load local UI from ui/ folder
        Path html = Paths.get(System.getProperty("user.dir"), "ui", "index.html");
        if (!html.toFile().exists()) {
            html = Paths.get(System.getProperty("user.dir"), "..", "ui", "index.html").normalize();
        }
        engine.load(html.toUri().toString());

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
                // push initial state
                bridge.pushFullState();
            }
        });

        Scene scene = new Scene(webView, 1280, 800);
        stage.setTitle("ThanOS");
        stage.initStyle(StageStyle.DECORATED);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        stage.setOnCloseRequest(e -> {
            store.saveAll();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

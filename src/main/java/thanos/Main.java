package thanos;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import thanos.ui.DesktopController;
import thanos.ui.LoginScreen;

/**
 * ThanOS — Dynamic Workspace Desktop Environment
 *
 * Priority: Dynamic Workspaces that genuinely preserve and restore state.
 * Data Structures: Linked List · HashMap · Queue · Stack
 * Fully offline.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1280, 800);

        // Start with login
        LoginScreen login = new LoginScreen(() -> {
            // transition to desktop
            DesktopController desk = new DesktopController(stage);
            var desktop = desk.buildDesktop();
            root.getChildren().setAll(desktop);
            desk.setScene(scene);
        });

        root.getChildren().add(login.build());

        // load dark theme by default for login too
        var css = getClass().getResource("/css/dark.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("ThanOS — Dynamic Workspace Environment");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(640);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

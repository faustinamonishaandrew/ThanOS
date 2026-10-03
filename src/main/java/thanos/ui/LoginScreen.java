package thanos.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple Windows-style login screen.
 * Default PIN is 1234 (shown as hint for demo convenience).
 */
public class LoginScreen {

    private final Runnable onSuccess;

    public LoginScreen(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public StackPane build() {
        StackPane root = new StackPane();
        root.getStyleClass().add("login-root");

        // background gradient acts as wallpaper
        VBox center = new VBox(18);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(340);
        center.setPadding(new Insets(40));
        center.getStyleClass().add("login-card");

        Label time = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyleClass().add("login-time");

        Label date = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")));
        date.getStyleClass().add("login-date");

        Label avatar = new Label("👤");
        avatar.getStyleClass().add("login-avatar");

        Label user = new Label("ThanOS User");
        user.getStyleClass().add("login-user");

        PasswordField pin = new PasswordField();
        pin.setPromptText("PIN  (demo: 1234)");
        pin.getStyleClass().add("login-pin");
        pin.setMaxWidth(220);

        Label error = new Label("");
        error.getStyleClass().add("login-error");

        Button login = new Button("Sign in");
        login.getStyleClass().add("login-btn");
        login.setPrefWidth(220);

        Runnable attempt = () -> {
            String p = pin.getText();
            // accept empty or 1234 for easy demo
            if (p.isEmpty() || p.equals("1234")) {
                FadeTransition ft = new FadeTransition(Duration.millis(300), root);
                ft.setFromValue(1); ft.setToValue(0);
                ft.setOnFinished(e -> onSuccess.run());
                ft.play();
            } else {
                error.setText("Incorrect PIN — try 1234 or leave blank");
                pin.clear();
            }
        };
        login.setOnAction(e -> attempt.run());
        pin.setOnAction(e -> attempt.run());

        center.getChildren().addAll(time, date, avatar, user, pin, login, error);
        root.getChildren().add(center);
        return root;
    }
}

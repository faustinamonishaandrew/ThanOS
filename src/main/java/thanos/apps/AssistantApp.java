package thanos.apps;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import thanos.services.AssistantService;

public class AssistantApp {
    private final AssistantService ai;
    private final TextArea chat = new TextArea();
    private final TextField input = new TextField();

    public AssistantApp(AssistantService ai) { this.ai = ai; }

    public Region build() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(12));
        root.getStyleClass().add("app-root");

        Label title = new Label("✦  Assistant");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Local offline helper · no cloud AI");
        sub.getStyleClass().add("hint");

        chat.setEditable(false);
        chat.setWrapText(true);
        chat.getStyleClass().add("chat");
        VBox.setVgrow(chat, Priority.ALWAYS);
        chat.setText("Assistant: Hello. Ask about workspaces, apps, tasks, or data structures.\n");

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);
        input.setPromptText("Ask…");
        input.getStyleClass().add("field");
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setOnAction(e -> send());
        Button send = new Button("Send");
        send.getStyleClass().add("btn-accent");
        send.setOnAction(e -> send());
        row.getChildren().addAll(input, send);

        root.getChildren().addAll(title, sub, chat, row);
        return root;
    }

    private void send() {
        String q = input.getText().trim();
        if (q.isEmpty()) return;
        chat.appendText("\nYou: " + q + "\n");
        chat.appendText("Assistant: " + ai.respond(q) + "\n");
        input.clear();
        chat.setScrollTop(Double.MAX_VALUE);
    }
}

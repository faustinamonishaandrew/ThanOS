package thanos.apps;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class CalculatorApp {
    private final Label display = new Label("0");
    private double acc = 0;
    private String op = null;
    private boolean fresh = true;

    public Region build() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(14));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("app-root");

        display.getStyleClass().add("calc-display");
        display.setMaxWidth(Double.MAX_VALUE);
        display.setAlignment(Pos.CENTER_RIGHT);
        display.setPadding(new Insets(10));

        GridPane g = new GridPane();
        g.setHgap(6); g.setVgap(6); g.setAlignment(Pos.CENTER);
        String[][] keys = {
            {"C","±","%","÷"}, {"7","8","9","×"},
            {"4","5","6","−"}, {"1","2","3","+"}, {"0",".","="}
        };
        for (int r = 0; r < keys.length; r++) {
            for (int c = 0; c < keys[r].length; c++) {
                String k = keys[r][c];
                Button b = new Button(k);
                b.getStyleClass().add("calc-btn");
                if ("÷×−+=C%±".contains(k)) b.getStyleClass().add("calc-op");
                if (k.equals("=")) b.getStyleClass().add("calc-eq");
                b.setPrefSize(60, 44);
                b.setOnAction(e -> key(k));
                if (k.equals("0")) { GridPane.setColumnSpan(b, 2); b.setPrefWidth(126); g.add(b, c, r); }
                else if (r == 4 && k.equals(".")) g.add(b, 2, r);
                else if (r == 4 && k.equals("=")) g.add(b, 3, r);
                else g.add(b, c, r);
            }
        }
        root.getChildren().addAll(display, g);
        return root;
    }

    private void key(String k) {
        switch (k) {
            case "C" -> { acc = 0; op = null; fresh = true; display.setText("0"); }
            case "±" -> display.setText(fmt(-Double.parseDouble(display.getText())));
            case "%" -> display.setText(fmt(Double.parseDouble(display.getText()) / 100));
            case "+", "−", "×", "÷" -> { apply(); op = k; fresh = true; }
            case "=" -> { apply(); op = null; fresh = true; }
            case "." -> {
                if (fresh) { display.setText("0."); fresh = false; }
                else if (!display.getText().contains(".")) display.setText(display.getText() + ".");
            }
            default -> {
                if (fresh) { display.setText(k); fresh = false; }
                else {
                    String cur = display.getText();
                    display.setText(cur.equals("0") ? k : cur + k);
                }
            }
        }
    }

    private void apply() {
        double cur = Double.parseDouble(display.getText());
        if (op == null) { acc = cur; return; }
        switch (op) {
            case "+" -> acc += cur;
            case "−" -> acc -= cur;
            case "×" -> acc *= cur;
            case "÷" -> acc = cur == 0 ? 0 : acc / cur;
        }
        display.setText(fmt(acc));
    }

    private String fmt(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.format("%.8g", v);
    }
}

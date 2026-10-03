package thanos.apps;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

public class CalendarApp {
    private YearMonth current = YearMonth.now();
    private final Label monthLabel = new Label();
    private final GridPane grid = new GridPane();

    public Region build() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(14));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("app-root");

        HBox nav = new HBox(12);
        nav.setAlignment(Pos.CENTER);
        Button prev = new Button("◀");
        prev.getStyleClass().add("btn");
        prev.setOnAction(e -> { current = current.minusMonths(1); render(); });
        Button next = new Button("▶");
        next.getStyleClass().add("btn");
        next.setOnAction(e -> { current = current.plusMonths(1); render(); });
        monthLabel.getStyleClass().add("section-title");
        nav.getChildren().addAll(prev, monthLabel, next);

        grid.setHgap(4); grid.setVgap(4);
        grid.setAlignment(Pos.CENTER);

        Label today = new Label("Today: " + LocalDate.now());
        today.getStyleClass().add("hint");

        root.getChildren().addAll(nav, grid, today);
        render();
        return root;
    }

    private void render() {
        grid.getChildren().clear();
        monthLabel.setText(current.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + "  " + current.getYear());

        String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        for (int i = 0; i < 7; i++) {
            Label d = new Label(days[i]);
            d.getStyleClass().add("cal-header");
            d.setPrefWidth(42);
            d.setAlignment(Pos.CENTER);
            grid.add(d, i, 0);
        }

        LocalDate first = current.atDay(1);
        int startCol = first.getDayOfWeek().getValue() - 1; // Mon=0
        int daysInMonth = current.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int row = 1;
        for (int day = 1; day <= daysInMonth; day++) {
            int col = (startCol + day - 1) % 7;
            if (day > 1 && col == 0) row++;
            Label cell = new Label(String.valueOf(day));
            cell.setPrefSize(42, 32);
            cell.setAlignment(Pos.CENTER);
            cell.getStyleClass().add("cal-day");
            if (current.getYear() == today.getYear()
                    && current.getMonth() == today.getMonth()
                    && day == today.getDayOfMonth()) {
                cell.getStyleClass().add("cal-today");
            }
            grid.add(cell, col, row);
        }
    }
}

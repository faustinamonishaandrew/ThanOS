import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarManager {
    public static class Event {
        public String id;
        public LocalDate date;
        public String title;
        public String description;

        public Event(String id, LocalDate date, String title, String description) {
            this.id = id;
            this.date = date;
            this.title = title;
            this.description = description;
        }

        public String toJson() {
            return String.format(
                "{\"id\":\"%s\",\"date\":\"%s\",\"title\":\"%s\",\"description\":\"%s\"}",
                id, date.toString(), escape(title), escape(description)
            );
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        }
    }

    private final Map<String, Event> events = new LinkedHashMap<>();
    private int nextId = 1;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public List<Event> getEventsForDate(LocalDate date) {
        List<Event> r = new ArrayList<>();
        for (Event e : events.values()) if (e.date.equals(date)) r.add(e);
        return r;
    }

    public List<Event> getAllEvents() { return new ArrayList<>(events.values()); }

    public Event createEvent(LocalDate date, String title, String description) {
        String id = "evt-" + (nextId++);
        Event e = new Event(id, date, title == null || title.isBlank() ? "Event" : title.trim(),
                description == null ? "" : description);
        events.put(id, e);
        return e;
    }

    public boolean updateEvent(String id, LocalDate date, String title, String description) {
        Event e = events.get(id);
        if (e == null) return false;
        e.date = date;
        e.title = title == null || title.isBlank() ? "Event" : title.trim();
        e.description = description == null ? "" : description;
        return true;
    }

    public boolean deleteEvent(String id) {
        return events.remove(id) != null;
    }

    public String toJsonArray() {
        return "[" + events.values().stream().map(Event::toJson).collect(Collectors.joining(",")) + "]";
    }

    public String eventsForDateJson(String dateStr) {
        try {
            LocalDate d = LocalDate.parse(dateStr);
            return "[" + getEventsForDate(d).stream().map(Event::toJson).collect(Collectors.joining(",")) + "]";
        } catch (Exception e) {
            return "[]";
        }
    }

    public void save(Path path) {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            for (Event e : events.values()) {
                w.write(e.id + "\t" + e.date.format(FMT) + "\t" + esc(e.title) + "\t" + esc(e.description));
                w.newLine();
            }
        } catch (IOException ex) {
            System.err.println("Event save: " + ex.getMessage());
        }
    }

    public void load(Path path) {
        events.clear();
        if (!Files.exists(path)) return;
        try (BufferedReader r = Files.newBufferedReader(path)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t", 4);
                if (p.length >= 4) {
                    try {
                        LocalDate d = LocalDate.parse(p[1], FMT);
                        Event e = new Event(p[0], d, unesc(p[2]), unesc(p[3]));
                        events.put(e.id, e);
                        if (e.id.startsWith("evt-")) {
                            try {
                                int num = Integer.parseInt(e.id.substring(4));
                                if (num >= nextId) nextId = num + 1;
                            } catch (NumberFormatException ignored) {}
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException ex) {
            System.err.println("Event load: " + ex.getMessage());
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private static String unesc(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
    }
}

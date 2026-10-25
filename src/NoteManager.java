import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class NoteManager {
    public static class Note {
        public String id;
        public String title;
        public String content;
        public long modified;

        public Note(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.modified = System.currentTimeMillis();
        }

        public String toJson() {
            return String.format(
                "{\"id\":\"%s\",\"title\":\"%s\",\"content\":\"%s\",\"modified\":%d}",
                id, escape(title), escape(content), modified
            );
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        }
    }

    private final Map<String, Note> notes = new LinkedHashMap<>();
    private int nextId = 1;

    public List<Note> getAllNotes() { return new ArrayList<>(notes.values()); }
    public Note getNote(String id) { return notes.get(id); }

    public Note createNote(String title, String content) {
        String id = "note-" + (nextId++);
        Note n = new Note(id, title == null || title.isBlank() ? "Untitled" : title.trim(),
                content == null ? "" : content);
        notes.put(id, n);
        return n;
    }

    public boolean updateNote(String id, String title, String content) {
        Note n = notes.get(id);
        if (n == null) return false;
        n.title = title == null || title.isBlank() ? "Untitled" : title.trim();
        n.content = content == null ? "" : content;
        n.modified = System.currentTimeMillis();
        return true;
    }

    public boolean deleteNote(String id) {
        return notes.remove(id) != null;
    }

    public String toJsonArray() {
        return "[" + notes.values().stream().map(Note::toJson).collect(Collectors.joining(",")) + "]";
    }

    public void save(Path path) {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            for (Note n : notes.values()) {
                w.write(n.id + "\t" + esc(n.title) + "\t" + esc(n.content) + "\t" + n.modified);
                w.newLine();
            }
        } catch (IOException e) {
            System.err.println("Note save: " + e.getMessage());
        }
    }

    public void load(Path path) {
        notes.clear();
        if (!Files.exists(path)) return;
        try (BufferedReader r = Files.newBufferedReader(path)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t", 4);
                if (p.length >= 4) {
                    Note n = new Note(p[0], unesc(p[1]), unesc(p[2]));
                    try { n.modified = Long.parseLong(p[3]); } catch (NumberFormatException ignored) {}
                    notes.put(n.id, n);
                    if (n.id.startsWith("note-")) {
                        try {
                            int num = Integer.parseInt(n.id.substring(5));
                            if (num >= nextId) nextId = num + 1;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Note load: " + e.getMessage());
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

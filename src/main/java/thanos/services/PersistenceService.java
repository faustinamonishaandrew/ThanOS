package thanos.services;

import thanos.model.Note;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Local File I/O for notes, settings, and workspace metadata. */
public class PersistenceService {

    private static final String DIR = "thanos_data";
    private static final String NOTES = DIR + "/notes";
    private static final String SETTINGS = DIR + "/settings.properties";
    private static final String WS_FILE = DIR + "/workspaces.txt";

    public PersistenceService() {
        try { Files.createDirectories(Paths.get(NOTES)); }
        catch (IOException e) { System.err.println(e.getMessage()); }
    }

    // ---- Notes ----
    public void saveNote(Note note) {
        Path p = Paths.get(NOTES, note.getId() + ".txt");
        try (BufferedWriter w = Files.newBufferedWriter(p)) {
            w.write("TITLE:" + note.getTitle()); w.newLine();
            w.write("CREATED:" + note.getCreatedAt()); w.newLine();
            w.write("MODIFIED:" + note.getModifiedAt()); w.newLine();
            w.write("---"); w.newLine();
            w.write(note.getContent() != null ? note.getContent() : "");
        } catch (IOException e) { System.err.println(e.getMessage()); }
    }

    public List<Note> loadNotes() {
        List<Note> notes = new ArrayList<>();
        Path dir = Paths.get(NOTES);
        if (!Files.isDirectory(dir)) return notes;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path p : ds) {
                Note n = readNote(p);
                if (n != null) notes.add(n);
            }
        } catch (IOException ignored) {}
        notes.sort((a, b) -> b.getModifiedAt().compareTo(a.getModifiedAt()));
        return notes;
    }

    private Note readNote(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            if (lines.size() < 4) return null;
            String id = path.getFileName().toString().replace(".txt", "");
            String title = lines.get(0).startsWith("TITLE:") ? lines.get(0).substring(6) : "Untitled";
            StringBuilder body = new StringBuilder();
            boolean in = false;
            for (String l : lines) {
                if (in) { if (body.length() > 0) body.append("\n"); body.append(l); }
                if (l.equals("---")) in = true;
            }
            return new Note(id, title, body.toString());
        } catch (IOException e) { return null; }
    }

    public void deleteNote(String id) {
        try { Files.deleteIfExists(Paths.get(NOTES, id + ".txt")); }
        catch (IOException ignored) {}
    }

    // ---- Settings ----
    public void saveSetting(String key, String val) {
        Properties p = loadProps();
        p.setProperty(key, val);
        try (OutputStream out = Files.newOutputStream(Paths.get(SETTINGS))) {
            p.store(out, "ThanOS");
        } catch (IOException ignored) {}
    }

    public String loadSetting(String key, String def) {
        return loadProps().getProperty(key, def);
    }

    private Properties loadProps() {
        Properties p = new Properties();
        Path path = Paths.get(SETTINGS);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) { p.load(in); }
            catch (IOException ignored) {}
        }
        return p;
    }

    // ---- Workspace names (basic persistence of created workspaces) ----
    public void saveWorkspaceNames(List<String> names) {
        try {
            Files.write(Paths.get(WS_FILE), names);
        } catch (IOException ignored) {}
    }

    public List<String> loadWorkspaceNames() {
        Path p = Paths.get(WS_FILE);
        if (!Files.exists(p)) return List.of();
        try { return Files.readAllLines(p); }
        catch (IOException e) { return List.of(); }
    }
}

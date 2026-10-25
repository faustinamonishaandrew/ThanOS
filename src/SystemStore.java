import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SystemStore {
    private final AppManager appManager = new AppManager();
    private final WindowManager windowManager = new WindowManager();
    private final WindowHistory windowHistory = new WindowHistory();
    private final WorkspaceManager workspaceManager = new WorkspaceManager();
    private final NoteManager noteManager = new NoteManager();
    private final CalendarManager calendarManager = new CalendarManager();
    private final SettingsService settingsService = new SettingsService();
    private final RecentFilesQueue recentFiles = new RecentFilesQueue();
    private final Path dataDir;
    private final Path filesDir;

    public SystemStore() {
        dataDir = Paths.get(System.getProperty("user.home"), ".thanos");
        filesDir = dataDir.resolve("files");
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(filesDir);
        } catch (IOException e) {
            System.err.println("Data dir: " + e.getMessage());
        }
    }

    public AppManager getAppManager() { return appManager; }
    public WindowManager getWindowManager() { return windowManager; }
    public WindowHistory getWindowHistory() { return windowHistory; }
    public WorkspaceManager getWorkspaceManager() { return workspaceManager; }
    public NoteManager getNoteManager() { return noteManager; }
    public CalendarManager getCalendarManager() { return calendarManager; }
    public SettingsService getSettingsService() { return settingsService; }
    public RecentFilesQueue getRecentFiles() { return recentFiles; }
    public Path getDataDir() { return dataDir; }
    public Path getFilesDir() { return filesDir; }

    public void loadAll() {
        settingsService.load(dataDir.resolve("settings.properties"));
        noteManager.load(dataDir.resolve("notes.txt"));
        calendarManager.load(dataDir.resolve("events.txt"));
        loadWorkspaces(dataDir.resolve("workspaces.txt"));
        recentFiles.load(dataDir.resolve("recent.txt"));
        seedSampleFilesIfNeeded();
    }

    public void saveAll() {
        settingsService.save(dataDir.resolve("settings.properties"));
        noteManager.save(dataDir.resolve("notes.txt"));
        calendarManager.save(dataDir.resolve("events.txt"));
        saveWorkspaces(dataDir.resolve("workspaces.txt"));
        recentFiles.save(dataDir.resolve("recent.txt"));
    }

    /**
     * Copy bundled sample files into the sandbox only when the sandbox is empty.
     * Never overwrites user-created content.
     */
    private void seedSampleFilesIfNeeded() {
        try {
            boolean empty = true;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(filesDir)) {
                if (stream.iterator().hasNext()) empty = false;
            }
            if (!empty) return;

            Path samples = locateSamplesDir();
            if (samples == null || !Files.isDirectory(samples)) {
                System.err.println("Sample files not found; skipping seed.");
                return;
            }
            copyRecursive(samples, filesDir);
            System.out.println("Seeded sample files from " + samples);
        } catch (Exception e) {
            System.err.println("Seed samples: " + e.getMessage());
        }
    }

    private Path locateSamplesDir() {
        Path p = Paths.get(System.getProperty("user.dir"), "samples");
        if (Files.isDirectory(p)) return p;
        p = Paths.get(System.getProperty("user.dir"), "..", "samples").normalize();
        if (Files.isDirectory(p)) return p;
        try {
            Path code = Paths.get(SystemStore.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            p = code.resolve("samples");
            if (Files.isDirectory(p)) return p;
            p = code.resolve("..").resolve("samples").normalize();
            if (Files.isDirectory(p)) return p;
        } catch (Exception ignored) {}
        return null;
    }

    private void copyRecursive(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(source -> {
            try {
                Path relative = src.relativize(source);
                Path target = dest.resolve(relative.toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    if (target.getParent() != null) Files.createDirectories(target.getParent());
                    if (!Files.exists(target)) {
                        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                }
            } catch (IOException e) {
                System.err.println("Copy " + source + ": " + e.getMessage());
            }
        });
    }

    private void saveWorkspaces(Path path) {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            w.write("ACTIVE=" + workspaceManager.getActiveWorkspaceId());
            w.newLine();
            for (Workspace ws : workspaceManager.getAllWorkspaces()) {
                w.write("WS|" + ws.getId() + "|" + escape(ws.getName()));
                w.newLine();
            }
        } catch (IOException e) {
            System.err.println("WS save: " + e.getMessage());
        }
    }

    private void loadWorkspaces(Path path) {
        if (!Files.exists(path)) return;
        List<Workspace> list = new ArrayList<>();
        String active = "ws-main";
        try (BufferedReader r = Files.newBufferedReader(path)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("ACTIVE=")) active = line.substring(7);
                else if (line.startsWith("WS|")) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length >= 3) list.add(new Workspace(parts[1], unescape(parts[2])));
                }
            }
            workspaceManager.loadWorkspaces(list, active);
        } catch (IOException e) {
            System.err.println("WS load: " + e.getMessage());
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\|", "|").replace("\\\\", "\\");
    }
}

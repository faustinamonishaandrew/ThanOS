import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bridge exposed to JavaScript as window.javaBridge.
 * All public methods are callable from JS.
 */
public class JavaBridge {
    private final SystemStore store;
    private final WebEngine engine;

    public JavaBridge(SystemStore store, WebEngine engine) {
        this.store = store;
        this.engine = engine;
    }

    // ---- Auth (demo) ----
    public boolean unlock(String password) {
        return password != null && !password.isBlank();
    }

    // ---- Clock ----
    public String getTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
    }

    public String getTaskbarClock() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm  EEE d MMM"));
    }

    // ---- Theme ----
    public String getTheme() {
        return store.getSettingsService().getTheme().name();
    }

    public void setTheme(String theme) {
        try {
            store.getSettingsService().setTheme(SettingsService.Theme.valueOf(theme.toUpperCase()));
            store.saveAll();
        } catch (Exception ignored) {}
    }

    // ---- Apps (HashMap) ----
    public String getApps() {
        return store.getAppManager().toJsonArray();
    }

    public String launchApp(String appId) {
        AppDefinition def = store.getAppManager().getApp(appId); // HashMap lookup
        if (def == null) return "{\"error\":\"unknown app\"}";

        store.getWindowHistory().push(appId); // Stack push

        // Record built-in app in Recent Files Queue
        store.getRecentFiles().recordAccess("app:" + appId, def.getName(), true);
        store.saveAll();

        String wsId = store.getWorkspaceManager().getActiveWorkspaceId();
        WindowModel model = new WindowModel(appId, def.getName(), wsId);
        store.getWindowManager().addWindow(model); // LinkedList add

        Workspace ws = store.getWorkspaceManager().getWorkspace(wsId);
        if (ws != null) ws.addWindowId(model.getId());

        return model.toJson();
    }

    public void closeWindow(String windowId) {
        store.getWindowManager().removeWindow(windowId);
        Workspace ws = store.getWorkspaceManager().getActiveWorkspace();
        if (ws != null) ws.removeWindowId(windowId);
    }

    public void minimizeWindow(String windowId) {
        store.getWindowManager().minimizeWindow(windowId);
    }

    public void restoreWindow(String windowId) {
        store.getWindowManager().restoreWindow(windowId);
    }

    public void focusWindow(String windowId) {
        store.getWindowManager().focusWindow(windowId);
    }

    public String getWindows() {
        String wsId = store.getWorkspaceManager().getActiveWorkspaceId();
        return store.getWindowManager().toJsonArray(wsId);
    }

    // ---- History Stack ----
    public String historyBack() {
        String prev = store.getWindowHistory().pop();
        return prev == null ? "" : prev;
    }

    public int historySize() {
        return store.getWindowHistory().size();
    }

    // ---- Workspaces (kept for compatibility; UI no longer exposes them) ----
    public String getWorkspaces() {
        return store.getWorkspaceManager().toJsonArray();
    }

    public String getActiveWorkspaceId() {
        return store.getWorkspaceManager().getActiveWorkspaceId();
    }

    public String createWorkspace(String name) {
        Workspace ws = store.getWorkspaceManager().createWorkspace(name);
        store.saveAll();
        return ws.toJson();
    }

    public boolean switchWorkspace(String id) {
        return store.getWorkspaceManager().switchTo(id);
    }

    public boolean renameWorkspace(String id, String name) {
        boolean ok = store.getWorkspaceManager().renameWorkspace(id, name);
        if (ok) store.saveAll();
        return ok;
    }

    public boolean deleteWorkspace(String id) {
        boolean ok = store.getWorkspaceManager().deleteWorkspace(id, store.getWindowManager());
        if (ok) store.saveAll();
        return ok;
    }

    // ---- Recent Files Queue ----
    public String getRecentFiles() {
        return store.getRecentFiles().toJsonArray();
    }

    public int recentFilesSize() {
        return store.getRecentFiles().size();
    }

    /**
     * Open a recent entry: if app, launchApp; if file, return open metadata for JS.
     */
    public String openRecent(String pathOrId) {
        if (pathOrId == null) return "{\"error\":\"empty\"}";
        if (pathOrId.startsWith("app:")) {
            String appId = pathOrId.substring(4);
            return launchApp(appId);
        }
        // Ordinary file
        Path p = Paths.get(pathOrId);
        if (!Files.exists(p)) {
            store.getRecentFiles().removeByPath(pathOrId);
            store.saveAll();
            return "{\"error\":\"missing\",\"path\":\"" + escapeJson(pathOrId) + "\"}";
        }
        String name = p.getFileName().toString();
        store.getRecentFiles().recordAccess(pathOrId, name, false);
        store.saveAll();
        return openFileByAbsolute(pathOrId);
    }

    // ---- Notes ----
    public String getNotes() {
        return store.getNoteManager().toJsonArray();
    }

    public String createNote(String title, String content) {
        NoteManager.Note n = store.getNoteManager().createNote(title, content);
        store.saveAll();
        return n.toJson();
    }

    public boolean updateNote(String id, String title, String content) {
        boolean ok = store.getNoteManager().updateNote(id, title, content);
        if (ok) store.saveAll();
        return ok;
    }

    public boolean deleteNote(String id) {
        boolean ok = store.getNoteManager().deleteNote(id);
        if (ok) store.saveAll();
        return ok;
    }

    // ---- Calendar ----
    public String getEvents() {
        return store.getCalendarManager().toJsonArray();
    }

    public String getEventsForDate(String dateStr) {
        return store.getCalendarManager().eventsForDateJson(dateStr);
    }

    public String createEvent(String dateStr, String title, String description) {
        try {
            LocalDate d = LocalDate.parse(dateStr);
            CalendarManager.Event e = store.getCalendarManager().createEvent(d, title, description);
            store.saveAll();
            return e.toJson();
        } catch (Exception ex) {
            return "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}";
        }
    }

    public boolean deleteEvent(String id) {
        boolean ok = store.getCalendarManager().deleteEvent(id);
        if (ok) store.saveAll();
        return ok;
    }

    // ---- Calculator ----
    public String calc(String op, String aStr, String bStr) {
        try {
            double a = Double.parseDouble(aStr);
            double b = Double.parseDouble(bStr);
            double r;
            switch (op) {
                case "+" -> r = a + b;
                case "-" -> r = a - b;
                case "*" -> r = a * b;
                case "/" -> {
                    if (b == 0) return "Error";
                    r = a / b;
                }
                default -> { return "Error"; }
            }
            if (r == (long) r) return String.valueOf((long) r);
            return String.valueOf(r);
        } catch (Exception e) {
            return "Error";
        }
    }

    // ---- File Manager (real sandbox under ~/.thanos/files) ----
    private Path sandboxRoot() {
        return store.getFilesDir();
    }

    private Path resolveInSandbox(String relative) {
        Path sandbox = sandboxRoot();
        Path dir = (relative == null || relative.isBlank() || relative.equals("/"))
                ? sandbox : sandbox.resolve(relative).normalize();
        if (!dir.startsWith(sandbox)) return null;
        return dir;
    }

    public String listFiles(String relative) {
        Path dir = resolveInSandbox(relative);
        if (dir == null || !Files.isDirectory(dir)) return "[]";
        try (Stream<Path> stream = Files.list(dir)) {
            String json = stream.sorted()
                    .map(this::fileMetaJson)
                    .collect(Collectors.joining(","));
            return "[" + json + "]";
        } catch (Exception e) {
            return "[]";
        }
    }

    private String fileMetaJson(Path p) {
        try {
            String name = p.getFileName().toString();
            boolean isDir = Files.isDirectory(p);
            long size = isDir ? 0 : Files.size(p);
            long modified = Files.getLastModifiedTime(p).toMillis();
            String type = isDir ? "folder" : guessType(name);
            boolean isApp = name.toLowerCase().endsWith(".app");
            return String.format(
                "{\"name\":\"%s\",\"dir\":%s,\"size\":%d,\"modified\":%d,\"type\":\"%s\",\"isApp\":%s}",
                escapeJson(name), isDir, size, modified, type, isApp
            );
        } catch (Exception e) {
            return "{\"name\":\"?\",\"dir\":false,\"size\":0,\"modified\":0,\"type\":\"unknown\",\"isApp\":false}";
        }
    }

    private String guessType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".log")) return "text";
        if (lower.endsWith(".java") || lower.endsWith(".js") || lower.endsWith(".css") || lower.endsWith(".html") || lower.endsWith(".py") || lower.endsWith(".c") || lower.endsWith(".cpp") || lower.endsWith(".h")) return "code";
        if (lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yml") || lower.endsWith(".yaml")) return "data";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp")) return "image";
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".app")) return "app";
        return "file";
    }

    public boolean createFolder(String relative, String name) {
        return fileOp(relative, name, true);
    }

    public boolean createFile(String relative, String name) {
        return fileOp(relative, name, false);
    }

    private boolean fileOp(String relative, String name, boolean folder) {
        if (name == null || name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) return false;
        try {
            Path dir = resolveInSandbox(relative);
            if (dir == null) return false;
            Path target = dir.resolve(name);
            if (Files.exists(target)) return false;
            if (folder) Files.createDirectory(target);
            else Files.writeString(target, "", StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean renamePath(String relative, String oldName, String newName) {
        if (oldName == null || newName == null || newName.isBlank()
                || newName.contains("..") || newName.contains("/") || newName.contains("\\")) return false;
        try {
            Path dir = resolveInSandbox(relative);
            if (dir == null) return false;
            Path src = dir.resolve(oldName).normalize();
            Path dst = dir.resolve(newName).normalize();
            if (!src.startsWith(sandboxRoot()) || !dst.startsWith(sandboxRoot())) return false;
            if (!Files.exists(src) || Files.exists(dst)) return false;
            Files.move(src, dst);
            // Update recent if absolute path changed
            String oldAbs = src.toAbsolutePath().toString();
            String newAbs = dst.toAbsolutePath().toString();
            store.getRecentFiles().removeByPath(oldAbs);
            store.getRecentFiles().recordAccess(newAbs, newName, false);
            store.saveAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean copyPath(String relative, String name, String destRelative) {
        try {
            Path dir = resolveInSandbox(relative);
            Path destDir = resolveInSandbox(destRelative);
            if (dir == null || destDir == null) return false;
            Path src = dir.resolve(name).normalize();
            if (!src.startsWith(sandboxRoot()) || !Files.exists(src)) return false;
            Path dst = destDir.resolve(name).normalize();
            if (!dst.startsWith(sandboxRoot())) return false;
            if (Files.exists(dst)) {
                // unique name
                String base = name;
                String ext = "";
                int dot = name.lastIndexOf('.');
                if (dot > 0) { base = name.substring(0, dot); ext = name.substring(dot); }
                int i = 1;
                while (Files.exists(dst)) {
                    dst = destDir.resolve(base + " (" + i + ")" + ext);
                    i++;
                }
            }
            if (Files.isDirectory(src)) {
                copyDir(src, dst);
            } else {
                Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void copyDir(Path src, Path dst) throws Exception {
        Files.walk(src).forEach(s -> {
            try {
                Path r = src.relativize(s);
                Path t = dst.resolve(r.toString());
                if (Files.isDirectory(s)) Files.createDirectories(t);
                else {
                    if (t.getParent() != null) Files.createDirectories(t.getParent());
                    Files.copy(s, t, StandardCopyOption.COPY_ATTRIBUTES);
                }
            } catch (Exception ignored) {}
        });
    }

    public boolean deletePath(String relative, String name) {
        try {
            Path dir = resolveInSandbox(relative);
            if (dir == null) return false;
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(sandboxRoot()) || !Files.exists(target)) return false;
            String abs = target.toAbsolutePath().toString();
            if (Files.isDirectory(target)) {
                try (Stream<Path> s = Files.walk(target)) {
                    s.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                        try { Files.delete(p); } catch (Exception ignored) {}
                    });
                }
            } else {
                Files.delete(target);
            }
            store.getRecentFiles().removeByPath(abs);
            store.saveAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSandboxPath() {
        return sandboxRoot().toString();
    }

    /** Read text file content (relative to sandbox). */
    public String readTextFile(String relative, String name) {
        try {
            Path dir = resolveInSandbox(relative);
            if (dir == null) return "{\"error\":\"invalid path\"}";
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(sandboxRoot()) || !Files.isRegularFile(target)) {
                return "{\"error\":\"not found\"}";
            }
            long size = Files.size(target);
            if (size > 2_000_000) return "{\"error\":\"too large\"}";
            String content = Files.readString(target, StandardCharsets.UTF_8);
            // Record recent
            store.getRecentFiles().recordAccess(target.toAbsolutePath().toString(), name, false);
            store.saveAll();
            return "{\"ok\":true,\"content\":\"" + escapeJson(content) + "\",\"path\":\"" + escapeJson(target.toAbsolutePath().toString()) + "\"}";
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    public boolean writeTextFile(String relative, String name, String content) {
        try {
            Path dir = resolveInSandbox(relative);
            if (dir == null) return false;
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(sandboxRoot())) return false;
            Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
            store.getRecentFiles().recordAccess(target.toAbsolutePath().toString(), name, false);
            store.saveAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Open a file: returns JSON describing how to handle it.
     * For .app files, launches the corresponding built-in app.
     * For text/code/json: returns content.
     * For images: returns base64 data URL.
     * For pdf: returns path info (preview limited).
     */
    public String openFile(String relative, String name) {
        try {
            Path dir = resolveInSandbox(relative);
            if (dir == null) return "{\"error\":\"invalid path\"}";
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(sandboxRoot()) || !Files.exists(target)) {
                return "{\"error\":\"not found\"}";
            }
            if (Files.isDirectory(target)) {
                return "{\"type\":\"folder\",\"name\":\"" + escapeJson(name) + "\"}";
            }
            return openFileByAbsolute(target.toAbsolutePath().toString());
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String openFileByAbsolute(String absPath) {
        try {
            Path target = Paths.get(absPath);
            if (!Files.exists(target) || !Files.isRegularFile(target)) {
                return "{\"error\":\"not found\"}";
            }
            String name = target.getFileName().toString();
            String lower = name.toLowerCase();

            // .app shortcut -> launch built-in
            if (lower.endsWith(".app")) {
                String content = Files.readString(target, StandardCharsets.UTF_8).trim();
                String appId = null;
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("app=") || line.startsWith("id=")) {
                        appId = line.substring(line.indexOf('=') + 1).trim();
                        break;
                    }
                }
                if (appId == null) {
                    // fallback from filename
                    String base = name.substring(0, name.length() - 4).toLowerCase();
                    if (base.contains("note")) appId = "notes";
                    else if (base.contains("calc")) appId = "calculator";
                    else if (base.contains("calen")) appId = "calendar";
                    else if (base.contains("set")) appId = "settings";
                }
                if (appId != null && store.getAppManager().hasApp(appId)) {
                    return launchApp(appId);
                }
                return "{\"error\":\"unknown application shortcut\"}";
            }

            store.getRecentFiles().recordAccess(absPath, name, false);
            store.saveAll();

            String type = guessType(name);
            if ("image".equals(type)) {
                byte[] bytes = Files.readAllBytes(target);
                if (bytes.length > 5_000_000) return "{\"error\":\"image too large\"}";
                String mime = lower.endsWith(".png") ? "image/png"
                        : lower.endsWith(".gif") ? "image/gif"
                        : lower.endsWith(".webp") ? "image/webp"
                        : "image/jpeg";
                String b64 = Base64.getEncoder().encodeToString(bytes);
                return "{\"type\":\"image\",\"name\":\"" + escapeJson(name)
                        + "\",\"dataUrl\":\"data:" + mime + ";base64," + b64
                        + "\",\"path\":\"" + escapeJson(absPath) + "\"}";
            }
            if ("text".equals(type) || "code".equals(type) || "data".equals(type)) {
                if (Files.size(target) > 2_000_000) return "{\"error\":\"file too large\"}";
                String content = Files.readString(target, StandardCharsets.UTF_8);
                return "{\"type\":\"text\",\"name\":\"" + escapeJson(name)
                        + "\",\"content\":\"" + escapeJson(content)
                        + "\",\"path\":\"" + escapeJson(absPath) + "\",\"relativeName\":\"" + escapeJson(name) + "\"}";
            }
            if ("pdf".equals(type)) {
                return "{\"type\":\"pdf\",\"name\":\"" + escapeJson(name)
                        + "\",\"path\":\"" + escapeJson(absPath)
                        + "\",\"size\":" + Files.size(target) + "}";
            }
            return "{\"type\":\"binary\",\"name\":\"" + escapeJson(name)
                    + "\",\"path\":\"" + escapeJson(absPath)
                    + "\",\"size\":" + Files.size(target) + "}";
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    public String getWallpaperUrl(String theme) {
        // Return file URL to assets wallpaper so CSS can use it
        try {
            Path assets = locateAssetsDir();
            if (assets == null) return "";
            String file = "LIGHT".equalsIgnoreCase(theme) ? "wallpaper-light.jpg" : "wallpaper-dark.jpg";
            Path img = assets.resolve(file);
            if (!Files.exists(img)) return "";
            return img.toUri().toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Path locateAssetsDir() {
        Path p = Paths.get(System.getProperty("user.dir"), "assets");
        if (Files.isDirectory(p)) return p;
        p = Paths.get(System.getProperty("user.dir"), "..", "assets").normalize();
        if (Files.isDirectory(p)) return p;
        try {
            Path code = Paths.get(JavaBridge.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            p = code.resolve("assets");
            if (Files.isDirectory(p)) return p;
            p = code.resolve("..").resolve("assets").normalize();
            if (Files.isDirectory(p)) return p;
        } catch (Exception ignored) {}
        return null;
    }

    // ---- Stats (no queue demo) ----
    public String getStats() {
        return String.format(
            "{\"apps\":%d,\"windows\":%d,\"history\":%d,\"recent\":%d,\"theme\":\"%s\"}",
            store.getAppManager().getRegistry().size(),
            store.getWindowManager().getOpenCount(),
            store.getWindowHistory().size(),
            store.getRecentFiles().size(),
            store.getSettingsService().getTheme().name()
        );
    }

    public void saveAll() {
        store.saveAll();
    }

    /** Push full state to JS after load */
    public void pushFullState() {
        Platform.runLater(() -> {
            try {
                String theme = store.getSettingsService().getTheme().name();
                String wallpaper = getWallpaperUrl(theme);
                String script = String.format(
                    "if(window.onJavaReady) window.onJavaReady(%s, %s, '%s', '%s', '%s');",
                    store.getAppManager().toJsonArray(),
                    store.getWorkspaceManager().toJsonArray(),
                    store.getWorkspaceManager().getActiveWorkspaceId(),
                    theme,
                    escapeJs(wallpaper)
                );
                engine.executeScript(script);
            } catch (Exception e) {
                System.err.println("pushFullState: " + e.getMessage());
            }
        });
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}

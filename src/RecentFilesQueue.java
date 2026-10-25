import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Recent Files Queue – FIFO with max 7 distinct entries.
 * Backed by LinkedList implementing Queue.
 * Oldest at front, newest at back.
 * Re-access moves entry to newest position (no duplicates).
 * Persistence survives restarts. Does not delete real files.
 */
public class RecentFilesQueue {
    public static final int MAX_SIZE = 7;

    /** Entry: pathOrId is absolute file path, or "app:notes" for built-in apps. */
    public static class FileEntry {
        public final String pathOrId;   // stable key
        public final String displayName;
        public final boolean isApp;
        public final long accessedAt;

        public FileEntry(String pathOrId, String displayName, boolean isApp) {
            this.pathOrId = pathOrId;
            this.displayName = displayName != null ? displayName : pathOrId;
            this.isApp = isApp;
            this.accessedAt = System.currentTimeMillis();
        }

        public FileEntry(String pathOrId, String displayName, boolean isApp, long accessedAt) {
            this.pathOrId = pathOrId;
            this.displayName = displayName != null ? displayName : pathOrId;
            this.isApp = isApp;
            this.accessedAt = accessedAt;
        }

        public String toJson() {
            return String.format(
                "{\"pathOrId\":\"%s\",\"displayName\":\"%s\",\"isApp\":%s,\"accessedAt\":%d}",
                escape(pathOrId), escape(displayName), isApp, accessedAt
            );
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    private final Queue<FileEntry> queue = new LinkedList<>();

    /** Record access. Moves existing to back if already present; evicts oldest when full. */
    public synchronized void recordAccess(String pathOrId, String displayName, boolean isApp) {
        if (pathOrId == null || pathOrId.isBlank()) return;

        // Remove existing entry with same key (no duplicates)
        queue.removeIf(e -> pathOrId.equals(e.pathOrId));

        // Evict oldest if at capacity
        while (queue.size() >= MAX_SIZE) {
            queue.poll(); // remove from front (oldest)
        }

        queue.offer(new FileEntry(pathOrId, displayName, isApp));
    }

    /** Remove entry when the actual file is deleted. */
    public synchronized void removeByPath(String pathOrId) {
        if (pathOrId == null) return;
        queue.removeIf(e -> pathOrId.equals(e.pathOrId));
    }

    public synchronized List<FileEntry> getEntries() {
        // Return newest-first for UI convenience
        List<FileEntry> list = new ArrayList<>(queue);
        Collections.reverse(list);
        return list;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized void clear() {
        queue.clear();
    }

    public String toJsonArray() {
        List<FileEntry> entries = getEntries();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(entries.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    public void save(Path path) {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            for (FileEntry e : queue) {
                w.write((e.isApp ? "A" : "F") + "|" + e.pathOrId + "|" + escape(e.displayName) + "|" + e.accessedAt);
                w.newLine();
            }
        } catch (IOException ex) {
            System.err.println("RecentFiles save: " + ex.getMessage());
        }
    }

    public void load(Path path) {
        queue.clear();
        if (!Files.exists(path)) return;
        try (BufferedReader r = Files.newBufferedReader(path)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split("\\|", 4);
                if (parts.length >= 3) {
                    boolean isApp = parts[0].equals("A");
                    String pathOrId = parts[1];
                    String name = unescape(parts[2]);
                    long at = parts.length >= 4 ? Long.parseLong(parts[3]) : System.currentTimeMillis();
                    // Skip missing ordinary files
                    if (!isApp) {
                        try {
                            if (!Files.exists(Paths.get(pathOrId))) continue;
                        } catch (Exception ignore) { continue; }
                    }
                    queue.offer(new FileEntry(pathOrId, name, isApp, at));
                }
            }
            // Ensure max size after load
            while (queue.size() > MAX_SIZE) queue.poll();
        } catch (Exception ex) {
            System.err.println("RecentFiles load: " + ex.getMessage());
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

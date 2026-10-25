import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class SettingsService {
    public enum Theme { DARK, LIGHT }

    private Theme theme = Theme.DARK;
    private boolean animationsEnabled = true;

    public Theme getTheme() { return theme; }
    public void setTheme(Theme theme) { this.theme = theme; }
    public boolean isDark() { return theme == Theme.DARK; }
    public boolean isAnimationsEnabled() { return animationsEnabled; }
    public void setAnimationsEnabled(boolean v) { animationsEnabled = v; }

    public void save(Path path) {
        Properties p = new Properties();
        p.setProperty("theme", theme.name());
        p.setProperty("animations", String.valueOf(animationsEnabled));
        try (OutputStream out = Files.newOutputStream(path)) {
            p.store(out, "ThanOS Settings");
        } catch (IOException e) {
            System.err.println("Settings save: " + e.getMessage());
        }
    }

    public void load(Path path) {
        if (!Files.exists(path)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
            try {
                theme = Theme.valueOf(p.getProperty("theme", "DARK"));
            } catch (IllegalArgumentException e) {
                theme = Theme.DARK;
            }
            animationsEnabled = Boolean.parseBoolean(p.getProperty("animations", "true"));
        } catch (IOException e) {
            System.err.println("Settings load: " + e.getMessage());
        }
    }
}

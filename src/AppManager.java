import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application registry – HashMap for O(1) lookup by app ID.
 */
public class AppManager {
    private final Map<String, AppDefinition> registry = new HashMap<>();

    public AppManager() {
        registry.put("notes", new AppDefinition("notes", "Notes", "N", "Create and manage notes"));
        registry.put("calendar", new AppDefinition("calendar", "Calendar", "C", "Events and monthly view"));
        registry.put("calculator", new AppDefinition("calculator", "Calculator", "±", "Basic arithmetic"));
        registry.put("files", new AppDefinition("files", "File Manager", "F", "Browse local files"));
        registry.put("settings", new AppDefinition("settings", "Settings", "S", "Themes and preferences"));
    }

    public AppDefinition getApp(String id) {
        return registry.get(id);
    }

    public boolean hasApp(String id) {
        return registry.containsKey(id);
    }

    public Collection<AppDefinition> getAllApps() {
        return registry.values();
    }

    public Map<String, AppDefinition> getRegistry() {
        return registry;
    }

    public String toJsonArray() {
        return "[" + registry.values().stream()
                .map(AppDefinition::toJson)
                .collect(Collectors.joining(",")) + "]";
    }
}

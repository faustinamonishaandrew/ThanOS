public class AppDefinition {
    private final String id;
    private final String name;
    private final String icon;
    private final String description;

    public AppDefinition(String id, String name, String icon, String description) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"icon\":\"%s\",\"description\":\"%s\"}",
            escape(id), escape(name), escape(icon), escape(description)
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

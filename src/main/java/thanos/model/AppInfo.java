package thanos.model;

/** Static definition of an installable application. Stored in AppRegistry (HashMap). */
public class AppInfo {
    private final String id;          // e.g. "notes"
    private final String name;        // e.g. "Notes"
    private final String glyph;       // icon character
    private final String description;

    public AppInfo(String id, String name, String glyph, String description) {
        this.id = id;
        this.name = name;
        this.glyph = glyph;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getGlyph() { return glyph; }
    public String getDescription() { return description; }

    @Override
    public String toString() { return glyph + "  " + name; }
}

package thanos.model;

public class Note {
    private final String id;
    private String title;
    private String content;
    private final String createdAt;
    private String modifiedAt;

    public Note(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
        String now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.createdAt = now;
        this.modifiedAt = now;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { title = t; touch(); }
    public String getContent() { return content; }
    public void setContent(String c) { content = c; touch(); }
    public String getCreatedAt() { return createdAt; }
    public String getModifiedAt() { return modifiedAt; }

    private void touch() {
        modifiedAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    @Override
    public String toString() { return title; }
}

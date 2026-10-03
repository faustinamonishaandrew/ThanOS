package thanos.model;

/** A pending task belonging to a specific workspace. Stored in TaskQueue. */
public class WorkspaceTask {
    private final String id;
    private final String description;
    private final String createdAt;
    private Status status;

    public enum Status { PENDING, DONE }

    public WorkspaceTask(String id, String description) {
        this.id = id;
        this.description = description;
        this.createdAt = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        this.status = Status.PENDING;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }

    @Override
    public String toString() {
        return "[" + status + "] " + description + "  @" + createdAt;
    }
}

public class WorkspaceAction {
    public enum Type {
        CREATE_WORKSPACE, SWITCH_WORKSPACE, RENAME_WORKSPACE, DELETE_WORKSPACE, OPEN_APP
    }

    private final Type type;
    private final String targetId;
    private final String payload;

    public WorkspaceAction(Type type, String targetId, String payload) {
        this.type = type;
        this.targetId = targetId;
        this.payload = payload;
    }

    public Type getType() { return type; }
    public String getTargetId() { return targetId; }
    public String getPayload() { return payload; }

    @Override
    public String toString() {
        return type + "[" + targetId + (payload != null ? "," + payload : "") + "]";
    }
}

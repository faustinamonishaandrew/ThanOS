import java.util.LinkedList;
import java.util.Queue;

/**
 * FIFO Queue for workspace operations.
 */
public class WorkspaceQueue {
    private final Queue<WorkspaceAction> queue = new LinkedList<>();

    public void enqueue(WorkspaceAction action) {
        if (action != null) queue.offer(action);
    }

    public WorkspaceAction dequeue() {
        return queue.poll();
    }

    public WorkspaceAction peek() {
        return queue.peek();
    }

    public boolean isEmpty() { return queue.isEmpty(); }
    public int size() { return queue.size(); }
    public Queue<WorkspaceAction> getQueue() { return queue; }
    public void clear() { queue.clear(); }
}

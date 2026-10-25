import java.util.Stack;

/**
 * LIFO history using java.util.Stack.
 */
public class WindowHistory {
    private final Stack<String> history = new Stack<>();
    private static final int MAX = 50;

    public void push(String itemId) {
        if (itemId == null || itemId.isBlank()) return;
        if (!history.isEmpty() && history.peek().equals(itemId)) return;
        history.push(itemId);
        while (history.size() > MAX) history.remove(0);
    }

    public String pop() {
        return history.isEmpty() ? null : history.pop();
    }

    public String peek() {
        return history.isEmpty() ? null : history.peek();
    }

    public boolean isEmpty() { return history.isEmpty(); }
    public int size() { return history.size(); }
    public Stack<String> getStack() { return history; }
    public void clear() { history.clear(); }
}

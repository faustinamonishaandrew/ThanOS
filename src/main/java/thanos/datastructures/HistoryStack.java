package thanos.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * LIFO Stack for workspace history navigation.
 * Push on every switch; pop to return to previous workspace.
 */
public class HistoryStack {

    private static class Node {
        String data; Node next;
        Node(String d) { data = d; }
    }

    private Node top;
    private int size;

    public void push(String name) {
        Node n = new Node(name);
        n.next = top; top = n; size++;
    }

    public String pop() {
        if (top == null) return null;
        String d = top.data; top = top.next; size--; return d;
    }

    public String peek() { return top != null ? top.data : null; }
    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    public List<String> toList() {
        List<String> list = new ArrayList<>();
        for (Node c = top; c != null; c = c.next) list.add(c.data);
        return list;
    }

    public void clear() { top = null; size = 0; }
}

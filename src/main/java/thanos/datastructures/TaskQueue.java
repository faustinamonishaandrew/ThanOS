package thanos.datastructures;

import thanos.model.WorkspaceTask;
import java.util.ArrayList;
import java.util.List;

/** FIFO Queue of pending tasks for one workspace. */
public class TaskQueue {

    private static class Node {
        WorkspaceTask data; Node next;
        Node(WorkspaceTask d) { data = d; }
    }

    private Node front, rear;
    private int size;

    public void enqueue(WorkspaceTask t) {
        Node n = new Node(t);
        if (rear == null) { front = rear = n; }
        else { rear.next = n; rear = n; }
        size++;
    }

    public WorkspaceTask dequeue() {
        if (front == null) return null;
        WorkspaceTask d = front.data;
        front = front.next;
        if (front == null) rear = null;
        size--;
        return d;
    }

    public WorkspaceTask peek() { return front != null ? front.data : null; }
    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    public List<WorkspaceTask> all() {
        List<WorkspaceTask> list = new ArrayList<>();
        for (Node c = front; c != null; c = c.next) list.add(c.data);
        return list;
    }

    public void clear() { front = rear = null; size = 0; }
}

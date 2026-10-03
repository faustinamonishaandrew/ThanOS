package thanos.datastructures;

import thanos.model.RunningApp;
import java.util.ArrayList;
import java.util.List;

/**
 * Singly Linked List of running applications inside ONE workspace.
 * Insert on launch, remove on close, traverse for taskbar / Task Manager.
 */
public class ActiveAppList {

    private static class Node {
        RunningApp data;
        Node next;
        Node(RunningApp d) { data = d; }
    }

    private Node head;
    private int size;

    public void insert(RunningApp app) {
        Node n = new Node(app);
        n.next = head;
        head = n;
        size++;
    }

    public boolean remove(String instanceId) {
        if (head == null) return false;
        if (head.data.getInstanceId().equals(instanceId)) {
            head = head.next; size--; return true;
        }
        Node prev = head, curr = head.next;
        while (curr != null) {
            if (curr.data.getInstanceId().equals(instanceId)) {
                prev.next = curr.next; size--; return true;
            }
            prev = curr; curr = curr.next;
        }
        return false;
    }

    public RunningApp find(String instanceId) {
        Node c = head;
        while (c != null) {
            if (c.data.getInstanceId().equals(instanceId)) return c.data;
            c = c.next;
        }
        return null;
    }

    public List<RunningApp> toList() {
        List<RunningApp> list = new ArrayList<>(size);
        Node c = head;
        while (c != null) { list.add(c.data); c = c.next; }
        return list;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() { head = null; size = 0; }
}

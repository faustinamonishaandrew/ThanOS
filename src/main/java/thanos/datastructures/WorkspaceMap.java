package thanos.datastructures;

import thanos.model.Workspace;
import java.util.ArrayList;
import java.util.List;

/** HashMap: workspace name → Workspace. Core of Dynamic Workspace lookup. */
public class WorkspaceMap {

    private static class Entry {
        String key; Workspace value; Entry next;
        Entry(String k, Workspace v) { key = k; value = v; }
    }

    private static final int CAP = 16;
    private final Entry[] buckets = new Entry[CAP];
    private int size;

    private int hash(String k) {
        int h = 0;
        for (char c : k.toCharArray()) h = (h * 31 + Character.toLowerCase(c)) % CAP;
        return Math.abs(h);
    }

    public void put(String name, Workspace ws) {
        int i = hash(name);
        for (Entry e = buckets[i]; e != null; e = e.next)
            if (e.key.equalsIgnoreCase(name)) { e.value = ws; return; }
        Entry n = new Entry(name.toLowerCase(), ws);
        n.next = buckets[i]; buckets[i] = n; size++;
    }

    public Workspace get(String name) {
        for (Entry e = buckets[hash(name)]; e != null; e = e.next)
            if (e.key.equalsIgnoreCase(name)) return e.value;
        return null;
    }

    public boolean contains(String name) { return get(name) != null; }

    public boolean remove(String name) {
        int i = hash(name);
        Entry prev = null, curr = buckets[i];
        while (curr != null) {
            if (curr.key.equalsIgnoreCase(name)) {
                if (prev == null) buckets[i] = curr.next;
                else prev.next = curr.next;
                size--; return true;
            }
            prev = curr; curr = curr.next;
        }
        return false;
    }

    /** Rename key in the map (remove old, put under new name). */
    public boolean rename(String oldName, String newName) {
        Workspace ws = get(oldName);
        if (ws == null || contains(newName)) return false;
        remove(oldName);
        ws.setName(newName);
        put(newName, ws);
        return true;
    }

    public List<Workspace> all() {
        List<Workspace> list = new ArrayList<>();
        for (Entry b : buckets)
            for (Entry e = b; e != null; e = e.next) list.add(e.value);
        return list;
    }

    public List<String> names() {
        List<String> n = new ArrayList<>();
        for (Workspace w : all()) n.add(w.getName());
        return n;
    }

    public int size() { return size; }
}

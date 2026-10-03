package thanos.datastructures;

import thanos.model.AppInfo;
import java.util.ArrayList;
import java.util.List;

/** HashMap: app id → AppInfo. Separate chaining. */
public class AppRegistry {

    private static class Entry {
        String key; AppInfo value; Entry next;
        Entry(String k, AppInfo v) { key = k; value = v; }
    }

    private static final int CAP = 32;
    private final Entry[] buckets = new Entry[CAP];
    private int size;

    private int hash(String k) {
        int h = 0;
        for (char c : k.toCharArray()) h = (h * 31 + Character.toLowerCase(c)) % CAP;
        return Math.abs(h);
    }

    public void put(String id, AppInfo info) {
        int i = hash(id);
        for (Entry e = buckets[i]; e != null; e = e.next)
            if (e.key.equalsIgnoreCase(id)) { e.value = info; return; }
        Entry n = new Entry(id.toLowerCase(), info);
        n.next = buckets[i]; buckets[i] = n; size++;
    }

    public AppInfo get(String id) {
        for (Entry e = buckets[hash(id)]; e != null; e = e.next)
            if (e.key.equalsIgnoreCase(id)) return e.value;
        return null;
    }

    public List<AppInfo> all() {
        List<AppInfo> list = new ArrayList<>();
        for (Entry b : buckets)
            for (Entry e = b; e != null; e = e.next) list.add(e.value);
        return list;
    }

    public int size() { return size; }
}

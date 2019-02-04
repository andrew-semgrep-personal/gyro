package beam.lang.ast.scope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Scope implements Map<String, Object> {

    private final Scope parent;
    private final Map<String, Object> values;

    public Scope(Scope parent, Map<String, Object> values) {
        this.parent = parent;
        this.values = values != null ? values : new LinkedHashMap<>();
    }

    public Scope(Scope parent) {
        this(parent, null);
    }

    public Scope getParent() {
        return parent;
    }

    @SuppressWarnings("unchecked")
    public <S extends Scope> S getClosest(Class<S> scopeClass) {
        for (Scope s = this; s != null; s = s.getParent()) {
            if (scopeClass.isInstance(s)) {
                return (S) s;
            }
        }

        return null;
    }

    public RootScope getRootScope() {
        return getClosest(RootScope.class);
    }

    public FileScope getFileScope() {
        return getClosest(FileScope.class);
    }

    @SuppressWarnings("unchecked")
    public void addValue(String key, Object value) {
        Object oldValue = get(key);
        List<Object> list;

        if (oldValue == null) {
            list = new ArrayList<>();

        } else if (oldValue instanceof List) {
            list = (List<Object>) oldValue;

        } else {
            list = new ArrayList<>();
            list.add(oldValue);
        }

        list.add(value);
        put(key, list);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values.containsValue(value);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public Object get(Object key) {
        return values.get(key);
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public Object put(String key, Object value) {
        return values.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> map) {
        values.putAll(map);
    }

    @Override
    public Object remove(Object key) {
        return values.remove(key);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public Collection<Object> values() {
        return values.values();
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Map && values.equals(other));
    }

    @Override
    public String toString() {
        return values.toString();
    }

}

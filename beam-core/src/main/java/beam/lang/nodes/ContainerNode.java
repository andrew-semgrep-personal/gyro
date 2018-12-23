package beam.lang.nodes;

import beam.lang.BeamLanguageException;
import com.google.common.base.CaseFormat;
import com.google.common.base.Throwables;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContainerNode extends Node {

    transient Map<ResourceKey, ResourceNode> resources = new HashMap<>();
    transient Map<String, ValueNode> keyValues = new HashMap<>();

    /**
     * Returns a map of key/value pairs for this block.
     *
     * This map is the internal state with the properties (from subclasses)
     * overlaid.
     *
     * The values are after resolution.
     */
    public Map<String, Object> resolvedKeyValues() {
        Map<String, Object> values = new HashMap<>();

        for (String key : keys()) {
            values.put(key, get(key).getValue());
        }

        try {
            for (PropertyDescriptor p : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {
                Method reader = p.getReadMethod();

                try {
                    Field propertyField = getClass().getDeclaredField(p.getName());
                    if (Modifier.isTransient(propertyField.getModifiers())) {
                        continue;
                    }
                } catch (NoSuchFieldException ex) {
                    continue;
                }

                if (reader != null) {
                    String key = keyFromFieldName(p.getDisplayName());
                    Object value = reader.invoke(this);

                    values.put(key, value);
                }
            }
        } catch (IllegalAccessException | IntrospectionException error) {
            throw new IllegalStateException(error);
        } catch (InvocationTargetException error) {
            throw Throwables.propagate(error);
        }

        return values;
    }

    public Set<String> keys() {
        return keyValues.keySet();
    }

    public ValueNode get(String key) {
        return keyValues.get(key);
    }

    public void put(String key, ValueNode valueNode) {
        valueNode.setParentNode(this);

        keyValues.put(key, valueNode);
    }

    public Collection<ResourceNode> resources() {
        return resources.values();
    }

    public ResourceNode removeResource(ResourceNode block) {
        return resources.remove(block.resourceKey());
    }

    public void putResource(ResourceNode resourceBlock) {
        resourceBlock.setParentNode(this);

        resources.put(resourceBlock.resourceKey(), resourceBlock);
    }

    public ResourceNode getResource(String key, String type) {
        ResourceKey resourceKey = new ResourceKey(type, key);
        return resources.get(resourceKey);
    }

    public void copyNonResourceState(ContainerNode source) {
        keyValues.putAll(source.keyValues);
    }

    @Override
    public boolean resolve() {
        for (ResourceNode resourceBlock : resources.values()) {
            boolean resolved = resourceBlock.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", resourceBlock);
            }
        }

        for (ValueNode value : keyValues.values()) {
            boolean resolved = value.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", value);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (ResourceNode resourceBlock : resources.values()) {
            sb.append(resourceBlock.toString());
        }

        for (Map.Entry<String, Object> entry : resolvedKeyValues().entrySet()) {
            Object value = entry.getValue();

            if (value != null) {
                sb.append("    ").append(entry.getKey()).append(": ");

                if (value instanceof String) {
                    sb.append("'" + entry.getValue() + "'");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(entry.getValue());
                } else if (value instanceof Map) {
                    sb.append(mapToString((Map) value));
                } else if (value instanceof List) {
                    sb.append(listToString((List) value));
                } else if (value instanceof ResourceNode) {
                    sb.append(((ResourceNode) value).resourceKey());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    protected String fieldNameFromKey(String key) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
    }

    protected String keyFromFieldName(String field) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field).replaceFirst("get-", "");
    }

    protected String valueToString(Object value) {
        StringBuilder sb = new StringBuilder();

        if (value instanceof String) {
            sb.append("'" + value + "'");
        } else if (value instanceof Map) {
            sb.append(mapToString((Map) value));
        } else if (value instanceof List) {
            sb.append(listToString((List) value));
        }

        return sb.toString();
    }

    protected String mapToString(Map map) {
        StringBuilder sb = new StringBuilder();

        sb.append("{").append("\n");

        for (Object key : map.keySet()) {
            Object value = map.get(key);

            sb.append("        ");
            sb.append(key).append(": ");
            sb.append(valueToString(value));
            sb.append(",\n");
        }
        sb.setLength(sb.length() - 2);

        sb.append("\n    }");

        return sb.toString();
    }

    protected String listToString(List list) {
        StringBuilder sb = new StringBuilder();

        sb.append("[").append("\n");

        for (Object value : list) {
            sb.append("        ");
            sb.append(valueToString(value));
            sb.append(",\n");
        }
        sb.setLength(sb.length() - 2);

        sb.append("\n    ]");

        return sb.toString();
    }

}

package gyro.core.resource;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.lang.ast.block.DirectiveNode;

@Type("extends")
public class ExtendsDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    @SuppressWarnings("unchecked")
    public void process(DiffableScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        Object source = getArgument(scope, node, Object.class, 0);
        Map<String, Object> sourceMap;

        if (source == null) {
            throw new GyroException("Can't extend from a null!");

        } else if (source instanceof Map) {
            sourceMap = (Map<String, Object>) source;

        } else if (source instanceof Resource) {
            sourceMap = DiffableInternals.getScope((Resource) source);

        } else {
            throw new GyroException(String.format(
                "Can't extend from @|bold %s|@ because it's an instance of @|bold %s|@!",
                source,
                source.getClass().getName()));
        }

        sourceMap.forEach((key, value) -> scope.put(key, merge(scope.get(key), value)));
    }

    private Object merge(Object oldValue, Object newValue) {
        if (oldValue instanceof Collection && newValue instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> clone = (Collection<Object>) clone(oldValue);

            ((Collection<?>) newValue).stream()
                .map(this::clone)
                .forEach(clone::add);

            return clone;

        } else if (oldValue instanceof Map && newValue instanceof Map) {
            Map<?, ?> oldMap = (Map<?, ?>) oldValue;
            Map<?, ?> newMap = (Map<?, ?>) newValue;

            return Stream.of(oldMap, newMap)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .distinct()
                .collect(
                    LinkedHashMap::new,
                    (map, key) -> map.put(clone(key), merge(clone(oldMap.get(key)), newMap.get(key))),
                    Map::putAll);

        } else if (oldValue != null) {
            return oldValue;

        } else {
            return clone(newValue);
        }
    }

    private Object clone(Object value) {
        if (value instanceof Diffable) {
            Diffable diffable = (Diffable) value;
            DiffableType<Diffable> type = DiffableType.getInstance(diffable);
            DiffableScope scope = diffable.scope;
            Diffable clone = type.newInstance(new DiffableScope(scope, null));

            DiffableInternals.setName(clone, diffable.name);
            type.setValues(clone, scope);
            return clone;

        } else if (value instanceof DiffableScope) {
            DiffableScope scope = (DiffableScope) value;
            DiffableScope clone = new DiffableScope(scope.getParent(), scope.getBlock());

            clone.putAll(scope);
            return clone;

        } if (value instanceof List) {
            return ((List<?>) value).stream()
                .map(this::clone)
                .collect(Collectors.toList());

        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).entrySet()
                .stream()
                .collect(
                    LinkedHashMap::new,
                    (m, e) -> m.put(clone(e.getKey()), clone(e.getValue())),
                    LinkedHashMap::putAll);

        } else if (value instanceof Set) {
            return ((Set<?>) value).stream()
                .map(this::clone)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        } else {
            return value;
        }
    }

}

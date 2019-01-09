package beam.core;

import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceDisplayDiff;
import beam.core.diff.ResourceName;
import beam.lang.ReferenceNode;
import beam.lang.ResourceNode;
import com.psddev.dari.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class BeamResource extends ResourceNode implements Comparable<BeamResource> {

    private transient BeamCredentials resourceCredentials;

    private transient ResourceChange change;

    public abstract boolean refresh();

    public abstract void create();

    public abstract void update(BeamResource current, Set<String> changedProperties);

    public abstract void delete();

    public abstract String toDisplayString();

    public abstract Class resourceCredentialsClass();

    @Override
    public BeamResource copy() {
        BeamResource resource = (BeamResource) super.copy();
        resource.setResourceCredentials(getResourceCredentials());

        return resource;
    }

    public String resourceCredentialsName() {
        Class c = resourceCredentialsClass();

        try {
            BeamCredentials credentials = (BeamCredentials) c.newInstance();

            String resourceNamespace = credentials.getCloudName();
            String resourceName = c.getSimpleName();
            if (c.isAnnotationPresent(ResourceName.class)) {
                ResourceName name = (ResourceName) c.getAnnotation(ResourceName.class);
                resourceName = name.value();

                return String.format("%s::%s", resourceNamespace, resourceName);
            }
        } catch (Exception ex) {
            throw new BeamException("Unable to determine credentials resource name.");
        }

        return c.getSimpleName();
    }

    public String primaryKey() {
        return String.format("%s %s", resourceType(), resourceIdentifier());
    }

    public BeamCredentials getResourceCredentials() {
        return resourceCredentials;
    }

    public void setResourceCredentials(BeamCredentials resourceCredentials) {
        this.resourceCredentials = resourceCredentials;
    }

    public ResourceChange change() {
        return change;
    }

    public void setChange(ResourceChange change) {
        this.change = change;
    }

    public void diffOnCreate(ResourceChange change) throws Exception {
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : subresourceFields()) {
            Object pendingValue = pendingValues.get(key);

            if (pendingValue instanceof Collection) {
                change.create((Collection) pendingValue);
            } else {
                change.createOne((BeamResource) pendingValue);
            }
        }
    }

    public void diffOnUpdate(ResourceChange change, BeamResource current) throws Exception {
        Map<String, Object> currentValues = current.resolvedKeyValues();
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : subresourceFields()) {

            Object currentValue = currentValues.get(key);
            Object pendingValue = pendingValues.get(key);

            if (pendingValue instanceof Collection) {
                change.update((Collection) currentValue, (Collection) pendingValue);
            } else {
                change.updateOne((BeamResource) currentValue, (BeamResource) pendingValue);
            }
        }
    }

    public void diffOnDelete(ResourceChange change) throws Exception {
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : subresourceFields()) {
            Object pendingValue = pendingValues.get(key);

            if (pendingValue instanceof Collection) {
                change.delete((Collection) pendingValue);
            } else {
                change.deleteOne((BeamResource) pendingValue);
            }
        }
    }

    public ResourceDisplayDiff calculateFieldDiffs(BeamResource current) {
        boolean firstField = true;

        ResourceDisplayDiff displayDiff = new ResourceDisplayDiff();

        Map<String, Object> currentValues = current.resolvedKeyValues();
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : pendingValues.keySet()) {
            // If there is no getter for this method then skip this field since there can
            // be no ResourceDiffProperty annotation.
            Method reader = readerMethodForKey(key);
            if (reader == null) {
                continue;
            }

            // If no ResourceDiffProperty annotation or if this field has subresources then skip this field.
            ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
            if (propertyAnnotation == null || propertyAnnotation.subresource()) {
                continue;
            }
            boolean nullable = propertyAnnotation.nullable();

            Object currentValue = currentValues.get(key);
            Object pendingValue = pendingValues.get(key);

            if (pendingValue != null || nullable) {
                String fieldChangeOutput = null;
                if (pendingValue instanceof List) {
                    fieldChangeOutput = ResourceChange.processAsListValue(key, (List) currentValue, (List) pendingValue);
                } else if (pendingValue instanceof Map) {
                    fieldChangeOutput = ResourceChange.processAsMapValue(key, (Map) currentValue, (Map) pendingValue);
                } else {
                    fieldChangeOutput = ResourceChange.processAsScalarValue(key, currentValue, pendingValue);
                }

                if (!ObjectUtils.isBlank(fieldChangeOutput)) {
                    if (!firstField) {
                        displayDiff.getChangedDisplay().append(", ");
                    }

                    displayDiff.addChangedProperty(key);
                    displayDiff.getChangedDisplay().append(fieldChangeOutput);

                    if (!propertyAnnotation.updatable()) {
                        displayDiff.setReplace(true);
                    }

                    firstField = false;
                }
            }
        }

        return displayDiff;
    }

    @Override
    public void execute() {
        if (get("resource-credentials") == null) {
            ReferenceNode credentialsReference = new ReferenceNode(resourceCredentialsName(), "default");
            credentialsReference.setLine(getLine());
            credentialsReference.setColumn(getColumn());

            put("resource-credentials", credentialsReference);
        }
    }

    @Override
    public int compareTo(BeamResource o) {
        if (o == null) {
            return 1;
        }

        String compareKey = String.format("%s %s", resourceType(), resourceIdentifier());
        String otherKey = String.format("%s %s", o.resourceType(), o.resourceIdentifier());

        return compareKey.compareTo(otherKey);
    }

    /**
     * Return a list of fields that contain subresources (ResourceDiffProperty(subresource = true)).
     */
    private List<String> subresourceFields() {
        List<String> keys = new ArrayList<>();
        Map<String, Object> pendingValues = resolvedKeyValues();

        for (String key : pendingValues.keySet()) {
            // If there is no getter for this method then skip this field since there can
            // be no ResourceDiffProperty annotation.
            Method reader = readerMethodForKey(key);
            if (reader == null) {
                continue;
            }

            // If no ResourceDiffProperty annotation or if this field is not subresources then skip this field.
            ResourceDiffProperty propertyAnnotation = reader.getAnnotation(ResourceDiffProperty.class);
            if (propertyAnnotation == null || !propertyAnnotation.subresource()) {
                continue;
            }

            keys.add(key);
        }

        return keys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BeamResource that = (BeamResource) o;

        return Objects.equals(primaryKey(), that.primaryKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryKey());
    }

}

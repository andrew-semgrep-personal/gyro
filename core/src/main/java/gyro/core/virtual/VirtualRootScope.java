package gyro.core.virtual;

import com.google.common.collect.ImmutableSet;
import gyro.core.resource.Resource;
import gyro.core.resource.RootScope;

public class VirtualRootScope extends RootScope {

    private final String prefix;

    public VirtualRootScope(RootScope scope, String prefix) {
        super(scope.getFile(), scope.getBackend(), null, ImmutableSet.of());
        this.prefix = prefix;
        putAll(scope);
        getFileScopes().addAll(scope.getFileScopes());
    }

    @Override
    public Resource findResource(String fullName) {
        String[] names = fullName.split("::");
        names[names.length - 1] = prefix + "/" + names[names.length - 1];
        return super.findResource(String.join("::", names));
    }

    @Override
    public void load() {
        throw new UnsupportedOperationException();
    }
}

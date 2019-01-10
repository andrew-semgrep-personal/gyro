package beam.lang;

import beam.core.BeamLocalState;
import beam.core.BeamState;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeamFile extends ResourceContainer {

    private transient String path;
    private transient BeamFile state;
    private BeamState stateBackend;
    private List<Provider> providers;

    transient Map<String, BeamFile> imports = new HashMap<>();

    public String path() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public BeamFile state() {
        if (state == null) {
            return this;
        }

        return state;
    }

    public void setState(BeamFile state) {
        this.state = state;
    }

    public BeamState stateBackend() {
        if (stateBackend == null) {
            return new BeamLocalState();
        }

        return stateBackend;
    }

    public void stateBackend(BeamState stateBackend) {
        this.stateBackend = stateBackend;
    }

    public List<Provider> providers() {
        if (providers == null) {
            providers = new ArrayList<>();
        }

        return providers;
    }

    public Map<String, BeamFile> imports() {
        return imports;
    }

    public void putImport(String key, BeamFile fileNode) {
        fileNode.setParentNode(this);

        imports.put(key, fileNode);
    }

    public BeamFile getImport(String key) {
        return imports.get(key);
    }

    public String importPath(String currentPath) {
        Path importPath = new File(path).toPath();
        Path otherPath  = new File(currentPath).getParentFile().toPath();

        return otherPath.relativize(importPath).toString().replace(".bcl", "");
    }

    @Override
    public Resource getResource(String type, String key) {
        Resource resource = super.getResource(type, key);
        if (resource == null && imports().containsKey("_")) {
            // Check in "global" import
            BeamFile importNode = imports().get("_");
            resource = importNode.getResource(type, key);
        }

        return resource;
    }

    @Override
    public void copyNonResourceState(Container source) {
        super.copyNonResourceState(source);

        if (source instanceof BeamFile) {
            BeamFile fileNode = (BeamFile) source;

            imports().putAll(fileNode.imports());
            providers().addAll(fileNode.providers());
            stateBackend(fileNode.stateBackend());
        }
    }

    @Override
    public Value get(String key) {
        Value value = super.get(key);
        if (value == null && imports().containsKey("_")) {
            // Check in "global" import
            BeamFile importNode = imports().get("_");
            value = importNode.get(key);
        }

        return value;
    }

    @Override
    public boolean resolve() {
        super.resolve();

        for (Resource resource : resources()) {
            boolean resolved = resource.resolve();
            if (!resolved) {
                throw new BeamLanguageException("Unable to resolve configuration.", resource);
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (String importName : imports.keySet()) {
            BeamFile importNode = imports.get(importName);

            String importPath = importNode.importPath(path) + ".bcl.state";

            sb.append("import ");
            sb.append(importPath);

            if (!importName.equals(importPath)) {
                sb.append(" as ");
                sb.append(importName);
            }

            sb.append("\n");
        }

        for (Provider provider : providers()) {
            sb.append(provider);
        }

        sb.append("\n");
        sb.append(super.toString());

        return sb.toString();
    }

}

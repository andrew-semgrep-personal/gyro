package beam.fetcher;

import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.extensions.ResourceExtension;
import beam.lang.BCL;
import com.psddev.dari.util.StringUtils;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class MavenFetcher extends PluginFetcher {

    private static String MAVEN_KEY = "^(?<group>[^:]+):(?<artifactId>[^:]+):(?<version>[^:]+)";
    private static Pattern MAVEN_KEY_PAT = Pattern.compile(MAVEN_KEY);

    @Override
    public boolean validate(String key) {
        return MAVEN_KEY_PAT.matcher(key).find();
    }

    @Override
    public void fetch(String key) {
        try {
            DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

            RepositorySystem system = locator.getService(RepositorySystem.class);
            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
            LocalRepository localRepo = new LocalRepository(System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository");
            session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

            RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
            RemoteRepository gradle = new RemoteRepository.Builder("gradle", "default", "https://repo.gradle.org/gradle/libs-releases/").build();

            Artifact artifact = new DefaultArtifact(key);

            CollectRequest collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), Arrays.asList(central, gradle));
            DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);
            DependencyRequest request = new DependencyRequest(collectRequest, filter);
            DependencyResult result = system.resolveDependencies(session, request);

            ClassLoader parent = getClass().getClassLoader();
            List<URL> urls = new ArrayList<>();
            File artifactFile = null;
            for (ArtifactResult artifactResult : result.getArtifactResults()) {
                if (artifactFile == null) {
                    artifactFile = artifactResult.getArtifact().getFile();
                }

                urls.add(new URL("file:///" + artifactResult.getArtifact().getFile()));
            }

            URL[] url = urls.toArray(new URL[0]);
            URLClassLoader loader = new URLClassLoader(url, parent);

            JarFile jarFile = new JarFile(artifactFile);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if(entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                String className = StringUtils.removeEnd(entry.getName(), ".class");
                className = className.replace('/', '.');
                Class c = loader.loadClass(className);
                if (BeamResource.class.isAssignableFrom(c)) {
                    if (!Modifier.isAbstract(c.getModifiers())) {
                        BCL.addExtension(c.getSimpleName(), new ResourceExtension(c));
                    }
                }
            }
        } catch (Exception e) {
            throw new BeamException("Maven fetch failed!", e);
        }
    }
}

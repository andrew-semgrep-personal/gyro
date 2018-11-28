package beam.lang;

import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import com.psddev.dari.util.ThreadLocalStack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BCL {

    private static Map<String, Class<? extends BeamConfig>> extensions = new HashMap<>();

    private static final ThreadLocalStack<Formatter> UI = new ThreadLocalStack<>();

    public static Map<String, Class<? extends BeamConfig>> getExtensions() {
        return extensions;
    }

    public static Formatter ui() {
        return UI.get();
    }

    public static void addExtension(String alias, Class<? extends BeamConfig> extension) {
        getExtensions().put(alias, extension);
    }

    public static void init() {
        UI.push(new Formatter());
        BCL.addExtension("for", ForConfig.class);
    }

    public static void shutdown() {
        UI.pop();
    }

    public static BeamConfig parse(String filename) {
        try {
            BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BeamParser parser = new BeamParser(tokens);
            BeamParser.BeamRootContext context = parser.beamRoot();

            File configFile = new File(filename);
            BeamConfig passingContext = new BeamConfig();

            BeamListener listener = new BeamListener(configFile.getCanonicalPath(), passingContext);
            ParseTreeWalker.DEFAULT.walk(listener, context);
            return listener.getConfig();

        } catch (Exception error) {
            error.printStackTrace();
        }

        return new BeamConfig();
    }

    public static void resolve(BeamConfig root) {
        boolean progress = true;
        while (progress) {
            progress = root.resolve(root);
        }
    }

    public static void getDependencies(BeamConfig root) {
        root.getDependencies(root);
    }
}

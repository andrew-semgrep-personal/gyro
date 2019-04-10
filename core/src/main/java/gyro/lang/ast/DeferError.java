package gyro.lang.ast;

import gyro.core.BeamException;
import gyro.lang.ast.scope.Scope;

import java.util.ArrayList;
import java.util.List;

public class DeferError extends Error {

    private final Node node;

    public static void evaluate(Scope scope, List<Node> body) throws Exception {
        int bodySize = body.size();

        while (true) {
            List<DeferError> errors = new ArrayList<>();
            List<Node> deferred = new ArrayList<>();

            for (Node node : body) {
                try {
                    node.evaluate(scope);

                } catch (DeferError error) {
                    errors.add(error);
                    deferred.add(node);
                }
            }

            if (deferred.isEmpty()) {
                break;

            } else if (bodySize == deferred.size()) {
                StringBuilder sb = new StringBuilder();
                for (DeferError error : errors) {
                    sb.append(error.getMessage());
                }

                throw new BeamException(sb.toString());

            } else {
                body = deferred;
                bodySize = body.size();
            }
        }
    }

    public DeferError(Node node) {
        this.node = node;
    }

    @Override
    public String getMessage() {
        return node.deferFailure();
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}

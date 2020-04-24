/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.core.workflow;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.core.GyroUI;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;
import gyro.lang.ast.Node;

public class UpdateAction extends Action {

    private final Node resource;
    private final List<Node> body;

    public UpdateAction(Node resource, List<Node> body) {
        this.resource = Preconditions.checkNotNull(resource);
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public Node getResource() {
        return resource;
    }

    public List<Node> getBody() {
        return body;
    }

    @Override
    public void execute(
        GyroUI ui,
        State state,
        Scope scope,
        List<String> toBeRemoved,
        List<ReplaceResource> toBeReplaced) {

        RootScope pending = scope.getRootScope();
        RootScope current = pending.getCurrent();

        Resource currentResource = visitResource(resource, current);
        ModifiedIn modifiedIn = DiffableInternals.getModifiedIn(currentResource) == ModifiedIn.WORKFLOW_ONLY
            ? ModifiedIn.WORKFLOW_ONLY
            : ModifiedIn.BOTH;
        DiffableInternals.setModifiedIn(currentResource, modifiedIn);

        Resource pendingResource = visitResource(resource, scope);
        DiffableInternals.setModifiedIn(pendingResource, modifiedIn);

        DiffableInternals.disconnect(pendingResource);
        DiffableScope resourceScope = DiffableInternals.getScope(pendingResource);
        NodeEvaluator evaluator = pending.getEvaluator();

        for (Node item : body) {
            evaluator.visit(item, resourceScope);
        }

        DiffableType.getInstance(pendingResource).setValues(pendingResource, resourceScope);
        DiffableInternals.update(pendingResource);
    }
}

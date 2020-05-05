/*
 * Copyright 2020, Perfect Sense, Inc.
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

package gyro.core.backend;

import gyro.core.LockBackend;
import gyro.core.Reflections;
import gyro.core.plugin.Plugin;
import gyro.core.scope.RootScope;

public class LockBackendPlugin extends Plugin {

    @Override
    public void onEachClass(RootScope root, Class<?> aClass) {
        if (LockBackend.class.isAssignableFrom(aClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends LockBackend> lockBackendClass = (Class<? extends LockBackend>) aClass;
            String namespace = Reflections.getNamespace(lockBackendClass);
            String type = Reflections.getType(lockBackendClass);

            root.getSettings(LockBackendSettings.class)
                .getLockBackendClasses()
                .put(namespace + "::" + type, lockBackendClass);
        }
    }

}

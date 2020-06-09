/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.persistence.ioc.provider.local;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.util.ThreadFactoryUtil;

import javax.inject.Provider;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author Christoph Schäbel
 */
@LazySingleton
public class PersistenceScheduledExecutorProvider implements Provider<ListeningScheduledExecutorService> {

    @Nullable
    private ListeningScheduledExecutorService executorService;

    @Override
    @LazySingleton
    @Persistence
    @NotNull
    public ListeningScheduledExecutorService get() {
        if (executorService == null) {
            final ThreadFactory threadFactory = ThreadFactoryUtil.create("scheduled-persistence-executor");
            final ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
            this.executorService = MoreExecutors.listeningDecorator(singleThreadExecutor);
        }
        return executorService;
    }
}

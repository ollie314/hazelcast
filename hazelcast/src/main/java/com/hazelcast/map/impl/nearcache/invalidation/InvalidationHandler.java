/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.impl.nearcache.invalidation;

/**
 * Handlers for various {@link Invalidation} implementations. An implementation of visitor pattern.
 */
public interface InvalidationHandler {

    /**
     * Handles a single key invalidation
     *
     * @param invalidation invalidation event
     */
    void handle(SingleNearCacheInvalidation invalidation);

    /**
     * Handles batch invalidations
     *
     * @param invalidation invalidation event
     */
    void handle(BatchNearCacheInvalidation invalidation);

    /**
     * Handles clear near-cache invalidation
     *
     * @param invalidation invalidation event
     */
    void handle(ClearNearCacheInvalidation invalidation);
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.core.common;


import org.drools.base.common.NetworkNode;
import org.drools.base.reteoo.NodeTypeEnums;
import org.kie.internal.runtime.StatefulKnowledgeSession;

/**
 * An interface for node memories implementation
 */
public interface NodeMemories {

    <T extends Memory> T getNodeMemory(MemoryFactory<T> node, ReteEvaluator reteEvaluator);

    void clearNodeMemory( MemoryFactory node );

    void clear();

    /**
     * Peeks at the content of the node memory for the given
     * node ID. This method has no side effects, so if the
     * given memory slot for the given node ID is null, it
     * will return null.
     *
     * @param memoryId
     * @return
     */
    Memory peekNodeMemory( int memoryId );

    default Memory peekNodeMemory(NetworkNode node) {
        return NodeTypeEnums.isMemoryFactory(node) ? peekNodeMemory(((MemoryFactory)node).getMemoryId()) : null;
    }

    /**
     * Returns the number of positions in this memory
     *
     * @return
     */
    int length();

    void resetAllMemories(StatefulKnowledgeSession session);
}

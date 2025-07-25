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
package org.kie.api.internal.assembler;

import java.util.List;

import org.kie.api.internal.utils.KieService;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceConfiguration;
import org.kie.api.io.ResourceType;
import org.kie.api.io.ResourceWithConfiguration;

public interface KieAssemblers extends KieService {

    void addResourceBeforeRules(
            Object knowledgeBuilder,
            Resource resource,
            ResourceType type,
            ResourceConfiguration configuration) throws Exception;

    void addResourceAfterRules(
            Object knowledgeBuilder,
            Resource resource,
            ResourceType type,
            ResourceConfiguration configuration) throws Exception;

    void addResourcesAfterRules(
            Object knowledgeBuilder,
            List<ResourceWithConfiguration> resources,
            ResourceType type) throws Exception;
}

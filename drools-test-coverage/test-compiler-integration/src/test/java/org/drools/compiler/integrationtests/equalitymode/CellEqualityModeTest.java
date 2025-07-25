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
package org.drools.compiler.integrationtests.equalitymode;

import java.util.stream.Stream;

import org.drools.compiler.integrationtests.AbstractCellTest;
import org.drools.testcoverage.common.util.EngineTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.TestParametersUtil2;

public class CellEqualityModeTest extends AbstractCellTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseConfigurations(EngineTestConfiguration.ALPHA_NETWORK_COMPILER_FALSE,
                                                           EngineTestConfiguration.EQUALITY_MODE,
                                                           EngineTestConfiguration.CLOUD_MODE,
                                                           EngineTestConfiguration.EXECUTABLE_MODEL_OFF,
                                                           EngineTestConfiguration.EXECUTABLE_MODEL_FLOW,
                                                           EngineTestConfiguration.EXECUTABLE_MODEL_PATTERN).stream();
    }
}

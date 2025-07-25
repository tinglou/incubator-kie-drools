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
package org.drools.testcoverage.regression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.drools.testcoverage.common.KieSessionTest;
import org.drools.testcoverage.common.model.TestEvent;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieSessionTestConfiguration;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.command.Command;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.io.Resource;
import org.mockito.exceptions.verification.WantedButNotInvoked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.fail;
import static org.drools.testcoverage.common.util.KieUtil.getCommands;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test suite for processing of facts imported using a "star" import and
 * declared in DRL at the same time.
 */
public class StarImportTest extends KieSessionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarImportTest.class);

    private static final String DRL_FILE = "star_import.drl";

    public static Stream<Arguments> parameters() {
        return TestParametersUtil2.getKieBaseAndStatefulKieSessionConfigurations().stream();
    }
    /**
     * Tests that rule fires if supplied with a fact that is imported using
     * "star" import.
     *
     * See BZ 973264.
     */
    
    @ParameterizedTest(name = "{1}" + " (from " + "{0}" + ")")
	@MethodSource("parameters")
    public void starImportedFactAlsoDeclaredInDRL(KieBaseTestConfiguration kieBaseTestConfiguration,
            KieSessionTestConfiguration kieSessionTestConfiguration) throws Exception {
    	createKieSession(kieBaseTestConfiguration, kieSessionTestConfiguration);
        session.setGlobal("LOGGER", LOGGER);
        AgendaEventListener ael = mock(AgendaEventListener.class);
        session.addEventListener(ael);
        List<Command<?>> commands = new ArrayList<Command<?>>();
        commands.add(getCommands().newInsert(new TestEvent(1, "event 1", 0)));
        commands.add(getCommands().newFireAllRules());

        session.execute(getCommands().newBatchExecution(commands, null));

        // the rule should have fired exactly once
        try {
            verify(ael, times(1)).afterMatchFired(any(AfterMatchFiredEvent.class));
        } catch (WantedButNotInvoked e) {
            fail("The rule does not fire. For more information see BZ 973264", e);
        }
    }

    @Override
    protected Resource[] createResources() {
        return KieUtil.createResources(DRL_FILE, StarImportTest.class);
    }
}

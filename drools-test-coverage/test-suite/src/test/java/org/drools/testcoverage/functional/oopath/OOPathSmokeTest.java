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
package org.drools.testcoverage.functional.oopath;

import java.util.stream.Stream;

import org.drools.testcoverage.common.model.Address;
import org.drools.testcoverage.common.model.Person;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests basic usage of OOPath expressions.
 */
public class OOPathSmokeTest {
    private static final KieServices KIE_SERVICES = KieServices.Factory.get();
    private static final ReleaseId RELEASE_ID = KIE_SERVICES.newReleaseId("org.drools.testcoverage.oopath", "marshalling-test", "1.0");

    private KieSession kieSession;

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseConfigurations().stream();
    }

    @AfterEach
    public void disposeKieSession() {
        if (this.kieSession != null) {
            this.kieSession.dispose();
            this.kieSession = null;
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testBuildKieBase(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final KieBase kieBase = KieBaseUtil.getKieBaseFromClasspathResources(this.getClass(), kieBaseTestConfiguration,
                "oopath.drl");
        assertThat(kieBase).isNotNull();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testBuildTwoKieBases() {
        final Resource drlResource = KIE_SERVICES.getResources().newClassPathResource("oopath.drl", this.getClass());
        KieUtil.getKieModuleFromResources(RELEASE_ID, KieBaseTestConfiguration.CLOUD_IDENTITY, drlResource);

        // creating two KieContainers and KieBases may trigger deep cloning
        for (int i = 0; i < 2; i++) {
            final KieContainer kieContainer = KIE_SERVICES.newKieContainer(RELEASE_ID);
            final KieBase kieBase = kieContainer.getKieBase();
            assertThat(kieBase).isNotNull();
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testFireRule(KieBaseTestConfiguration kieBaseTestConfiguration) {
        final KieBase kieBase = KieBaseUtil.getKieBaseFromClasspathResources(this.getClass(), kieBaseTestConfiguration,
                "oopath.drl");
        this.kieSession = kieBase.newKieSession();

        final Person person = new Person("Bruno", 21);
        person.setAddress(new Address("Some Street", 10, "Beautiful City"));
        this.kieSession.insert(person);
        assertThat(this.kieSession.fireAllRules()).isEqualTo(1);
    }

}

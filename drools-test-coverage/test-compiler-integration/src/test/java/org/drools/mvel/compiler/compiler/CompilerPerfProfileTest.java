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
package org.drools.mvel.compiler.compiler;

import java.io.IOException;
import java.io.InputStreamReader;

import org.drools.compiler.builder.impl.KnowledgeBuilderImpl;
import org.drools.base.definitions.InternalKnowledgePackage;
import org.drools.drl.parser.DroolsParserException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompilerPerfProfileTest {

    @Test
    public void testProfileRuns() throws Exception {

        //first run for warm up
        build("JDT", "largeRuleNumber.drl", false);
        build("MVEL", "largeRuleNumberMVEL.drl", false);

        System.gc();
        Thread.sleep( 100 );
        
        build("MVEL", "largeRuleNumberMVEL.drl", true);

        System.gc();
        Thread.sleep( 100 );

        
        build("JDT", "largeRuleNumber.drl", true);
        

        
        
        
    }

    private void build(String msg, String resource, boolean showResults) throws DroolsParserException,
                        IOException {
        final KnowledgeBuilderImpl builder = new KnowledgeBuilderImpl();
        long start = System.currentTimeMillis();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( resource ) ) );
        InternalKnowledgePackage pkg = builder.getPackage("org.drools.mvel.compiler.test");
        assertThat(builder.hasErrors()).isFalse();
        assertThat(pkg).isNotNull();
        if (showResults) {
            System.out.print( "Time taken for " + msg + " : " + (System.currentTimeMillis() - start) );
        }
    }
    
}

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
package org.kie.dmn.model.jandex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;
import org.kie.dmn.model.api.DMNModelInstrumentedBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class KieDMNModelJandexTest {

    public static final Logger LOG = LoggerFactory.getLogger(KieDMNModelJandexTest.class);
    private final String moduleName = "kie-dmn-model";

    @Test
    void reflectConfigJSON() throws Exception {
        Indexer indexer = new Indexer();
        InputStream stream = getClass().getClassLoader()
                                       .getResourceAsStream("org/kie/dmn/model/api/DMNModelInstrumentedBase.class");
        indexer.index(stream);
        stream.close();
        scanFile(new File("./target/classes"), indexer);
        Index index = indexer.complete();

        Set<ClassInfo> founds = index.getAllKnownImplementors(DotName.createSimple(DMNModelInstrumentedBase.class.getCanonicalName()));
        LOG.debug("founds: \n{}", founds);
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
        List<Object> results = new ArrayList<>();
        for (ClassInfo found : founds.stream().sorted(Comparator.comparing(ClassInfo::name)).collect(Collectors.toList())) {
            results.add(toReflectConfigMap(found));
        }
        LOG.debug("results: \n{}", results);
        String json = jsonb.toJson(results);
        LOG.debug("Expected reflect-config.json: \n{}", json);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fromJson = jsonb.fromJson(new FileReader("src/main/resources/META-INF/native-image/org.kie/" + moduleName + "/reflect-config.json"),
                                                            List.class);
        List<DotName> dotNamesInJSON = fromJson.stream().map(m -> DotName.createSimple((String) m.get("name"))).collect(Collectors.toList());

        Set<DotName> foundsViaJandex = founds.stream().map(ClassInfo::name).collect(Collectors.toSet());
        Set<DotName> foundsViaJSON = new HashSet<>(dotNamesInJSON);

        Set<DotName> missingExpected = new HashSet<>(foundsViaJSON);
        missingExpected.removeAll(foundsViaJandex);
        assertThat(missingExpected)
                .as("List of classes expected by JSON file and not found by JANDEX.")
                .isEmpty();
        Set<DotName> foundUnexpected = new HashSet<>(foundsViaJandex);
        foundUnexpected.removeAll(foundsViaJSON);
        assertThat(foundUnexpected)
                .as("List of classes found by JANDEX and not listed in JSON file.")
                .isEmpty();
        assertThat(foundsViaJandex)
                  .as("List of classes found via Jandex during test and listed in JSON file must be same.")
                  .isEqualTo(foundsViaJSON);
    }

    private Map<String, Object> toReflectConfigMap(ClassInfo found) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", found.name().toString());
        result.put("allDeclaredConstructors" , true);
        result.put("allPublicConstructors"   , true);
        result.put("allDeclaredMethods"      , true);
        result.put("allPublicMethods"        , true);
        result.put("allDeclaredClasses"      , true);
        result.put("allPublicClasses"        , true);
        return result;
    }

    private void scanFile(File source, Indexer indexer) throws Exception {
        if (source.isDirectory()) {
            File[] children = source.listFiles();
            if (children == null) {
                throw new FileNotFoundException("Source directory disappeared: " + source);
            }
            for (File child : children) {
                scanFile(child, indexer);
            }
            return;
        }

        if (!source.getName().endsWith(".class")) {
            return;
        }

        LOG.trace("scan: {}", source);
        try (FileInputStream input = new FileInputStream(source);) {
            indexer.index(input);
        }
    }
}

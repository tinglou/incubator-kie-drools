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
package org.kie.scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.core.util.FileManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.maven.integration.MavenRepository;

import static org.assertj.core.api.Assertions.assertThat;

public class KieModuleBuilderTest extends AbstractKieCiTest {

    private FileManager fileManager;

    @BeforeEach
    public void setUp() throws Exception {
        this.fileManager = new FileManager();
        this.fileManager.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.fileManager.tearDown();
    }

    @Test
    public void testKieModuleUsingPOMMissingKBaseDefinition() throws Exception {
        KieServices ks = KieServices.Factory.get();

        //Build a KieModule jar, deploy it into local Maven repository
        ReleaseId releaseId = ks.newReleaseId( "org.kie",
                                               "metadata-test2",
                                               "1.0-SNAPSHOT" );
        String pomText = getPom( releaseId );
        File pomFile = new File( System.getProperty( "java.io.tmpdir" ),
                                 MavenRepository.toFileName( releaseId, null ) + ".pom" );
        try {
            FileOutputStream fos = new FileOutputStream( pomFile );
            fos.write( pomText.getBytes() );
            fos.flush();
            fos.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writePomXML( getPom( releaseId ) );
        kfs.write( "src/main/java/org/kie/test/Bean.java",
                   createJavaSourceInPackage() );

        KieBuilder kieBuilder1 = ks.newKieBuilder( kfs );
        assertThat(kieBuilder1.buildAll().getResults().getMessages().isEmpty()).isTrue();
        InternalKieModule kieModule = (InternalKieModule) kieBuilder1.getKieModule();

        KieMavenRepository.getKieMavenRepository().installArtifact( releaseId, kieModule, pomFile );

        //Build a second KieModule, depends on the first KieModule jar which we have deployed into Maven
        ReleaseId releaseId2 = ks.newReleaseId( "org.kie",
                                                "metadata-test-using-pom",
                                                "1.0-SNAPSHOT" );
        String pomText2 = getPom( releaseId2,
                                  releaseId );
        File pomFile2 = new File( System.getProperty( "java.io.tmpdir" ),
                                  MavenRepository.toFileName( releaseId2, null ) + ".pom" );
        try {
            FileOutputStream fos = new FileOutputStream( pomFile2 );
            fos.write( pomText2.getBytes() );
            fos.flush();
            fos.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        //Try building the second KieModule
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        InputStream is = new FileInputStream( pomFile2 );
        KieModuleModel kproj2 = ks.newKieModuleModel();

        kieFileSystem.write( "pom.xml",
                             KieServices.Factory.get().getResources().newInputStreamResource( is ) );
        kieFileSystem.writeKModuleXML( kproj2.toXML() );
        kieFileSystem.write( "src/main/resources/rule.drl",
                             createDRLWithImport( "rule1" ) );
        KieBuilder kieBuilder = kieServices.newKieBuilder( kieFileSystem );
        kieBuilder.buildAll();
        assertThat(kieBuilder.getResults().getMessages().isEmpty()).isTrue();
    }

    private String createJavaSourceInPackage() {
        return "package org.kie.test;\n" +
                "public class Bean {\n" +
                "   private int value;\n" +
                "   public int getValue() {\n" +
                "       return value;\n" +
                "   }\n" +
                "}";
    }

    protected String createDRLWithImport( String ruleName ) {
        return "import org.kie.test.Bean\n" +
                "rule " + ruleName + "\n" +
                "when\n" +
                "Bean()\n" +
                "then\n" +
                "end\n";
    }

    @Test
    public void testPomTypeDependencies() throws Exception {
        // RHBPMS-4634
        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseIdNoDep = ks.newReleaseId( "org.kie", "test-no-dep", "1.0-SNAPSHOT" );
        ReleaseId releaseIdWithDep = ks.newReleaseId( "org.kie", "test-with-dep", "1.0-SNAPSHOT" );

        ReleaseId bomTypeDependency = ks.newReleaseId( "org.kie.ci.test", "kie-ci-test-bom", "1.2.3.Final" );

        String pom = getPomWithPomDependencies(releaseIdNoDep, bomTypeDependency);
        File pomFile = fileManager.newFile("pom.xml");
        fileManager.write(pomFile, pom);

        InternalKieModule kJar1 = createKieJarWithPomDependencies( ks, releaseIdNoDep, pom );
        KieMavenRepository repository = KieMavenRepository.getKieMavenRepository();
        repository.installArtifact(releaseIdNoDep, kJar1, pomFile);

        InternalKieModule kJar2 = createKieJarWithDependencies( ks, releaseIdWithDep, true, "rule1", releaseIdNoDep);
        KieContainer kieContainer2 = ks.newKieContainer( releaseIdWithDep );
        KieSession kieSession = kieContainer2.newKieSession();
    }

    private InternalKieModule createKieJarWithPomDependencies(KieServices ks, ReleaseId releaseId, String pom) throws IOException {
        KieFileSystem kfs = createKieFileSystemWithKProject(ks, false);
        kfs.writePomXML(pom);

        kfs.write("src/main/java/org/kie/test/Bean.java", createJavaSource(3));

        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        System.out.println(kieBuilder.getResults().getMessages());
        assertThat(kieBuilder.getResults().getMessages().isEmpty()).isTrue();
        return (InternalKieModule) kieBuilder.getKieModule();
    }

    private String getPomWithPomDependencies(ReleaseId releaseId, ReleaseId... dependencies) {
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "  <groupId>" + releaseId.getGroupId() + "</groupId>\n" +
                "  <artifactId>" + releaseId.getArtifactId() + "</artifactId>\n" +
                "  <version>" + releaseId.getVersion() + "</version>\n" +
                "\n";
        if (dependencies != null && dependencies.length > 0) {
            pom += "<dependencies>\n";
            for (ReleaseId dep : dependencies) {
                pom += "<dependency>\n";
                pom += "  <groupId>" + dep.getGroupId() + "</groupId>\n";
                pom += "  <artifactId>" + dep.getArtifactId() + "</artifactId>\n";
                pom += "  <version>" + dep.getVersion() + "</version>\n";
                pom += "  <type>pom</type>\n";
                pom += "</dependency>\n";
            }
            pom += "</dependencies>\n";
        }
        pom += "</project>";
        return pom;
    }

    protected String createDRL(String ruleName) {
        return "import org.kie.test.Bean\n" +
               "rule " + ruleName + "\n" +
               "when\n" +
               "  $b: Bean()\n" +
               "then\n" +
               "  System.out.println($b);" +
               "end\n";
    }
}

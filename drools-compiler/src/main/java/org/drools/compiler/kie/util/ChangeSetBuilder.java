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
package org.drools.compiler.kie.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.base.definitions.InternalKnowledgePackage;
import org.drools.compiler.builder.impl.KnowledgeBuilderFactoryServiceImpl;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.drl.ast.descr.BaseDescr;
import org.drools.drl.ast.descr.FunctionDescr;
import org.drools.drl.ast.descr.GlobalDescr;
import org.drools.drl.ast.descr.PackageDescr;
import org.drools.drl.ast.descr.RuleDescr;
import org.drools.drl.ast.descr.TypeDeclarationDescr;
import org.drools.drl.ast.descr.TypeFieldDescr;
import org.drools.drl.parser.DrlParser;
import org.drools.io.ByteArrayResource;
import org.drools.util.StringUtils;
import org.drools.util.TypeResolver;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.ChangeType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.ResourceChange;
import org.kie.internal.builder.ResourceChangeSet;
import org.kie.internal.builder.conf.DefaultPackageNameOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.compiler.builder.impl.KnowledgeBuilderConfigurationImpl.DEFAULT_PACKAGE;
import static org.drools.util.ClassUtils.convertClassToResourcePath;
import static org.drools.util.ClassUtils.convertResourceToClassName;
import static org.drools.util.StringUtils.isEmpty;

public class ChangeSetBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger( ChangeSetBuilder.class );

    private static String defaultPackageName;

    private ChangeSetBuilder() { }

    public static KieJarChangeSet build( InternalKieModule original, InternalKieModule currentJar ) {
        KieJarChangeSet result = new KieJarChangeSet();
        
        Collection<String> originalFiles = original.getFileNames();
        Collection<String> currentFiles = currentJar.getFileNames();
        
        ArrayList<String> removedFiles = new ArrayList<>( originalFiles );
        removedFiles.removeAll( currentFiles );
        if( ! removedFiles.isEmpty() ) {
            for( String file : removedFiles ) {
                // there should be a way to get the JAR name/url to produce a proper URL for the file in it
                result.removeFile( file );
            }
        }

        List<TypeDeclarationDescr> typeDeclarations = new ArrayList<>();
        Map<String, String> changedClasses = new HashMap<>();

        for( String file : currentFiles ) {
            if( originalFiles.contains( file ) ) {
                // check for modification
                byte[] ob = original.getBytes( file );
                byte[] cb = currentJar.getBytes( file );
                if( ! Arrays.equals( ob, cb ) ) {
                    if (file.endsWith( ".class" )) {
                        changedClasses.put(convertResourceToClassName(file), file);
                    } else if ( !ResourceType.DRL.matchesExtension(file) || !StringUtils.codeAwareEqualsIgnoreSpaces(new String(ob), new String(cb)) ) {
                        // check that: (NOT drl file) OR (NOT equalsIgnoringSpaces)
                        // parse the file to figure out the difference
                        result.registerChanges( file, diffResource( file, ob, cb, typeDeclarations ) );
                    }
                }
            } else {
                // file was added
                result.addFile( file );
            }
        }

        for (TypeDeclarationDescr typeDeclaration : typeDeclarations) {
            String fqn = typeDeclaration.getFullTypeName();
            if (changedClasses.containsKey( fqn )) {
                continue;
            }

            InternalKnowledgePackage pkg = original.getPackage( typeDeclaration.getNamespace() );
            if (pkg == null) {
                continue;
            }

            TypeResolver resolver = pkg.getTypeResolver();
            for (TypeFieldDescr field : typeDeclaration.getFields().values()) {
                String fieldType;
                try {
                    fieldType = resolver.resolveType( field.getPattern().getObjectType() ).getCanonicalName();
                } catch (ClassNotFoundException e) {
                    continue;
                }
                if (changedClasses.containsKey( fieldType )) {
                    changedClasses.put( fqn, convertClassToResourcePath( fqn ) );
                    break;
                }
            }
        }

        for (String changedClass : changedClasses.values()) {
            result.registerChanges( changedClass, new ResourceChangeSet( changedClass, ChangeType.UPDATED ) );
        }

        if (original.getKieModuleModel() != null) {
            for (String kieBaseName : original.getKieModuleModel().getKieBaseModels().keySet()) {
                KnowledgeBuilder originalKbuilder = original.getKnowledgeBuilderForKieBase( kieBaseName );
                if ( originalKbuilder != null && currentJar.getKnowledgeBuilderForKieBase( kieBaseName ) == null ) {
                    currentJar.cacheKnowledgeBuilderForKieBase( kieBaseName, originalKbuilder );
                }
            }
        }

        return result;
    }


    private static ResourceChangeSet diffResource(String file, byte[] originalBytes, byte[] currentBytes, List<TypeDeclarationDescr> typeDeclarations) {
        ResourceChangeSet pkgcs = new ResourceChangeSet( file, ChangeType.UPDATED );
        ResourceType type = ResourceType.determineResourceType( file );
        if( ResourceType.DRL.equals( type ) || ResourceType.GDRL.equals( type ) || ResourceType.RDRL.equals( type ) || ResourceType.TDRL.equals( type )) {
            try {
                PackageDescr originalPkg = new DrlParser().parse( new ByteArrayResource( originalBytes ) );
                PackageDescr currentPkg = new DrlParser().parse( new ByteArrayResource( currentBytes ) );
                String pkgName = isEmpty(currentPkg.getName()) ? getDefaultPackageName() : currentPkg.getName();
                String oldPkgName = isEmpty(originalPkg.getName()) ? getDefaultPackageName() : originalPkg.getName();

                if (!oldPkgName.equals(pkgName)) {
                    // if the package name is changed everthing has to be recreated from scratch
                    // so it is useless to further investigate other changes
                    return pkgcs;
                }
                pkgcs.setPackageName(pkgName);

                for( RuleDescr crd : currentPkg.getRules() ) {
                    pkgcs.getLoadOrder().add(new ResourceChangeSet.RuleLoadOrder(pkgName, crd.getName(), crd.getLoadOrder()));
                }

                List<RuleDescr> orules = new ArrayList<>( originalPkg.getRules() ); // needs to be cloned
                diffDescrs(originalBytes, currentBytes, pkgcs, orules, currentPkg.getRules(), ResourceChange.Type.RULE, RULE_CONVERTER);

                List<FunctionDescr> ofuncs = new ArrayList<>( originalPkg.getFunctions() ); // needs to be cloned
                diffDescrs(originalBytes, currentBytes, pkgcs, ofuncs, currentPkg.getFunctions(), ResourceChange.Type.FUNCTION, FUNC_CONVERTER);

                List<GlobalDescr> oglobals = new ArrayList<>( originalPkg.getGlobals() ); // needs to be cloned
                diffDescrs(originalBytes, currentBytes, pkgcs, oglobals, currentPkg.getGlobals(), ResourceChange.Type.GLOBAL, GLOBAL_CONVERTER);

                for (TypeDeclarationDescr typeDeclaration : currentPkg.getTypeDeclarations()) {
                    if ( isEmpty( typeDeclaration.getNamespace()) ) {
                        typeDeclaration.setNamespace( isEmpty( currentPkg.getNamespace() ) ? DEFAULT_PACKAGE : currentPkg.getNamespace() );
                    }
                    typeDeclarations.add( typeDeclaration );
                }
            } catch ( Exception e ) {
                logger.error( "Error analyzing the contents of "+file+". Skipping.", e );
            }
        }

        pkgcs.getChanges().sort( Comparator.comparingInt( r -> r.getChangeType().ordinal() ) );
        return pkgcs;
    }

    private interface DescrNameConverter<T extends BaseDescr> {
        String getName(T descr);
    }

    private static final RuleDescrNameConverter RULE_CONVERTER = new RuleDescrNameConverter();
    private static class RuleDescrNameConverter implements DescrNameConverter<RuleDescr> {
        @Override
        public String getName(RuleDescr descr) {
            return descr.getName();
        }
    }

    private static final FuncDescrNameConverter FUNC_CONVERTER = new FuncDescrNameConverter();
    private static class FuncDescrNameConverter implements DescrNameConverter<FunctionDescr> {
        @Override
        public String getName(FunctionDescr descr) {
            return descr.getName();
        }
    }

    private static final GlobalDescrNameConverter GLOBAL_CONVERTER = new GlobalDescrNameConverter();
    private static class GlobalDescrNameConverter implements DescrNameConverter<GlobalDescr> {
        @Override
        public String getName(GlobalDescr descr) {
            return descr.getIdentifier();
        }
    }

    private static <T extends BaseDescr> void diffDescrs(byte[] ob, byte[] cb,
                                                  ResourceChangeSet pkgcs,
                                                  List<T> odescrs, List<T> cdescrs,
                                                  ResourceChange.Type type, DescrNameConverter<T> descrNameConverter) {

        Set<String> updatedRules = null;
        if (type == ResourceChange.Type.RULE) {
            updatedRules = new HashSet<>();
            (( List<RuleDescr> ) cdescrs).sort( RULE_HIERARCHY_COMPARATOR );
        }

        for( T crd : cdescrs ) {
            String cName = descrNameConverter.getName(crd);

            // unfortunately have to iterate search for a rule with the same name
            boolean found = false;
            for( Iterator<T> it = odescrs.iterator(); it.hasNext(); ) {
                T ord = it.next();
                if( descrNameConverter.getName(ord).equals( cName ) ) {
                    found = true;
                    it.remove();

                    // using codeAwareEqualsIgnoreSpaces comparison because using the descriptor equals() method
                    // is brittle and heavier than iterating an array
                    if ( !StringUtils.codeAwareEqualsIgnoreSpaces(new String(Arrays.copyOfRange(ob, ord.getStartCharacter(), ord.getEndCharacter())),new String(Arrays.copyOfRange(cb, crd.getStartCharacter(), crd.getEndCharacter())) )
                         || (type == ResourceChange.Type.RULE && updatedRules.contains( ( (RuleDescr) crd ).getParentName() )) ) {
                        pkgcs.getChanges().add( new ResourceChange( ChangeType.UPDATED, type, cName ) );
                        if (type == ResourceChange.Type.RULE) {
                            updatedRules.add(cName);
                        }
                    }
                    break;
                }
            }
            if( !found ) {
                pkgcs.getChanges().add( new ResourceChange( ChangeType.ADDED, type, cName ) );
            }
        }

        for ( T ord : odescrs ) {
            pkgcs.getChanges().add( new ResourceChange( ChangeType.REMOVED,
                                                        type,
                                                        descrNameConverter.getName(ord) ) );
        }
    }

    private static final RuleHierarchyComparator RULE_HIERARCHY_COMPARATOR = new RuleHierarchyComparator();
    private static class RuleHierarchyComparator implements Comparator<RuleDescr> {
        @Override
        public int compare( RuleDescr r1, RuleDescr r2 ) {
            return r1.getName().equals( r2.getParentName() ) ? -1 : r2.getName().equals( r1.getParentName() ) ? 1 : 0;
        }
    }

    private static String getDefaultPackageName() {
        if (defaultPackageName == null) {
            KnowledgeBuilderConfiguration conf = new KnowledgeBuilderFactoryServiceImpl().newKnowledgeBuilderConfiguration();
            defaultPackageName = conf.getOption(DefaultPackageNameOption.KEY).packageName();
        }
        return defaultPackageName;
    }
}

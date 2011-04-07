/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.modello.tests;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;

public class ModelloGenerationTest
    extends AbstractLifecycleMappingTest
{
    public void test_p001_simple()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project = importProject( "projects/modello/modello-p001/pom.xml", configuration );
        waitForJobsToComplete();
        assertNoErrors( project );
        assertFalse( project.getFolder( "target/generated-sources/modello" ).exists() );

        project.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        project.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );
        waitForJobsToComplete();
        assertNoErrors( project );

        IJavaProject javaProject1 = JavaCore.create( project );
        IClasspathEntry[] cp1 = javaProject1.getRawClasspath();

        assertEquals( new Path( "/modello-p001/target/generated-sources/modello" ), cp1[3].getPath() );

        assertTrue( project.getFolder( "target/generated-sources/modello" ).exists() );
        assertTrue( project.getFile( "target/generated-sources/modello/generated/test/GeneratedTest.java" ).exists() );
        assertTrue( project.getFile( "target/generated-sources/modello/generated/test/GeneratedTest.java" ).isSynchronized( IResource.DEPTH_ZERO ) );
        assertTrue( project.getFile( "target/generated-sources/modello/generated/test/GeneratedTest.java" ).isAccessible() );

        // Test CLEAN
        project.build( IncrementalProjectBuilder.CLEAN_BUILD, monitor );
        waitForJobsToComplete();
        assertNoErrors( project );
        assertFalse( project.getFolder( "target/generated-sources/modello" ).exists() );
    }

    public void test_NoLifecycleMapping()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project1 = importProject( "projects/modello/modello-NoLifecycleMapping/pom.xml", configuration );
        waitForJobsToComplete();
        assertNoErrors( project1 );

        project1.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        project1.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );
        waitForJobsToComplete();
        assertNoErrors( project1 );

        IJavaProject javaProject1 = JavaCore.create( project1 );
        IClasspathEntry[] cp1 = javaProject1.getRawClasspath();

        assertEquals( new Path( "/modello-NoLifecycleMapping/target/generated-sources/modello" ), cp1[3].getPath() );

        assertTrue( project1.getFile( "target/generated-sources/modello/generated/test/GeneratedTest.java" ).isSynchronized( IResource.DEPTH_ZERO ) );
        assertTrue( project1.getFile( "target/generated-sources/modello/generated/test/GeneratedTest.java" ).isAccessible() );
    }

    public void test_IncompleteConfiguration()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project = importProject( "projects/modello/modello-IncompleteConfiguration/pom.xml", configuration );
        waitForJobsToComplete();

        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create( project, monitor );
        assertNotNull( "Expected not null MavenProjectFacade", facade );
        ILifecycleMapping lifecycleMapping = plugin.getProjectConfigurationManager().getLifecycleMapping( facade );
        assertNotNull( "Expected not null ILifecycleMapping", lifecycleMapping );
        List<MojoExecutionKey> notCoveredMojoExecutions = getNotCoveredMojoExecutions( facade );
        assertNotNull( notCoveredMojoExecutions );
        assertEquals( 1, notCoveredMojoExecutions.size() );
        MojoExecutionKey notCoveredMojoExecutionKey = notCoveredMojoExecutions.get( 0 );
        assertNotNull( notCoveredMojoExecutionKey );
        assertEquals( "org.sonatype.plugins", notCoveredMojoExecutionKey.getGroupId() );
        assertEquals( "modello-plugin-upgrade", notCoveredMojoExecutionKey.getArtifactId() );
        assertEquals( "0.0.1", notCoveredMojoExecutionKey.getVersion() );
        assertEquals( "upgrade", notCoveredMojoExecutionKey.getGoal() );
    }
}

/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.modello.tests;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;

public class ModelloGenerationTest
    extends AsbtractMavenProjectTestCase
{
    private MavenProjectManager mavenProjectManager;

    private IProjectConfigurationManager projectConfigurationManager;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        mavenProjectManager = MavenPlugin.getDefault().getMavenProjectManager();
        projectConfigurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    }

    protected void tearDown()
        throws Exception
    {
        projectConfigurationManager = null;
        mavenProjectManager = null;

        super.tearDown();
    }

    public void test_p001_simple()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project1 = importProject( "projects/p001/pom.xml", configuration );
        project1.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        waitForJobsToComplete();

        assertNoErrors( project1 );
    }
    
    private void assertNoErrors( IProject project ) throws CoreException
    {
        int severity = project.findMaxProblemSeverity( IMarker.PROBLEM, true, IResource.DEPTH_INFINITE );
        IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
        assertTrue( "Unexpected error markers " + toString( markers ), severity < IMarker.SEVERITY_ERROR );
    }

}

/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.buildhelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.sonatype.m2e.buildhelper.mojohandler.AddResourceBuildhelperMojoHandler;
import org.sonatype.m2e.buildhelper.mojohandler.AddSourceBuildhelperMojoHandler;
import org.sonatype.m2e.buildhelper.mojohandler.AddTestResourceBuildhelperMojoHandler;
import org.sonatype.m2e.buildhelper.mojohandler.AddTestSourceBuildhelperMojoHandler;
import org.sonatype.m2e.buildhelper.mojohandler.BuildhelperMojoHandler;

/**
 * {@link AbstractProjectConfigurator} used to support the <tt>Build Helper Maven Plugin</tt>. This class acts as a
 * mediator delegating the appropriate {@link BuildhelperMojoHandler}s.
 * 
 * @see BuildhelperMojoHandler
 * @author Igor Fedorenko
 * @author Phillip Webb
 */
public class BuildhelperProjectConfigurator
    extends AbstractProjectConfigurator
    implements IJavaProjectConfigurator
{
    /**
     * The delegate {@link BuildhelperMojoHandler}s.
     */
    private List<BuildhelperMojoHandler> handlers;

    public BuildhelperProjectConfigurator()
    {
        super();
        handlers = new ArrayList<BuildhelperMojoHandler>();
        handlers.add( new AddSourceBuildhelperMojoHandler() );
        handlers.add( new AddTestSourceBuildhelperMojoHandler() );
        handlers.add( new AddResourceBuildhelperMojoHandler() );
        handlers.add( new AddTestResourceBuildhelperMojoHandler() );
    }

    @Override
    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade, MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        return new BuildhelperBuildParticipant( handlers, execution );
    }

    public void configureRawClasspath( ProjectConfigurationRequest request, IClasspathDescriptor classpath,
                                       IProgressMonitor monitor )
        throws CoreException
    {
        assertHasNature( request.getProject(), JavaCore.NATURE_ID );
        for ( MojoExecution execution : getMojoExecutions( request, monitor ) )
        {
            for ( BuildhelperMojoHandler handler : handlers )
            {
                if ( handler.supports( execution ) )
                {
                    ConfigureRawClasspathContext context =
                        new ConfigureRawClasspathContextImpl( execution, request, classpath, monitor );
                    handler.configureRawClasspath( context );
                }
            }
        }
    }

    public void configureClasspath( IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor )
        throws CoreException
    {
        // No configuration is required here
    }

    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        // No configuration is required here
    }

    /**
     * Internal implementation of the {@link ConfigureRawClasspathContext}.
     */
    private class ConfigureRawClasspathContextImpl
        implements ConfigureRawClasspathContext
    {
        private MojoExecution execution;

        private ProjectConfigurationRequest request;

        private IClasspathDescriptor classpath;

        private IProgressMonitor monitor;

        private String resourceEncoding;

        private String testResourceEncoding;

        public ConfigureRawClasspathContextImpl( MojoExecution execution, ProjectConfigurationRequest request,
                                                 IClasspathDescriptor classpath, IProgressMonitor monitor )
            throws CoreException
        {
            this.execution = execution;
            this.request = request;
            this.classpath = classpath;
            this.monitor = monitor;
            this.resourceEncoding = determineResourceEncoding( "resources" );
            this.testResourceEncoding = determineResourceEncoding( "testResources" );
        }

        private String determineResourceEncoding( String resourcesGoal )
            throws CoreException
        {
            for ( MojoExecution resourcesMojoExecution : request.getMavenProjectFacade().getMojoExecutions( "maven-resources-plugin",
                                                                                                            "org.apache.maven.plugins",
                                                                                                            monitor,
                                                                                                            resourcesGoal ) )
            {
                String encoding =
                    maven.getMojoParameterValue( request.getMavenSession(), resourcesMojoExecution, "encoding",
                                                 String.class );
                if ( encoding != null )
                {
                    return encoding;
                }
            }
            return null;
        }

        public <T> T getParameterValue( String parameter, Class<T> asType )
            throws CoreException
        {
            return BuildhelperProjectConfigurator.this.getParameterValue( parameter, asType, request.getMavenSession(),
                                                                          execution );
        }

        public IPath getPath( File file )
        {
            IProject project = request.getMavenProjectFacade().getProject();
            IPath path = MavenProjectUtils.getProjectRelativePath( project, file.getAbsolutePath() );
            return project.getFullPath().append( path );
        }

        public IPath getPath( String relativeLocation )
        {
            return getPath( new File( request.getMavenProject().getBasedir(), relativeLocation ) );
        }

        public IClasspathDescriptor getClasspath()
        {
            return classpath;
        }

        public IPath getOutputLocation()
        {
            return request.getMavenProjectFacade().getOutputLocation();
        }

        public IPath getTestOutputLocation()
        {
            return request.getMavenProjectFacade().getTestOutputLocation();
        }

        public IProgressMonitor getMonitor()
        {
            return monitor;
        }

        public IProject getProject()
        {
            return request.getProject();
        }

        public String getResourceEncoding()
        {
            return resourceEncoding;
        }

        public String getTestResourceEncoding()
        {
            return testResourceEncoding;
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.mojos.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;

public abstract class AbstractJavaProjectConfigurator
    extends AbstractProjectConfigurator
    implements IJavaProjectConfigurator
{

    protected static class MojoExecutionKey
    {
        final String version;
        final String groupId;
        final String artifactId;
        final String goals;

        public MojoExecutionKey(String groupId, String artifactId, String versionRange, String goals)
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            version = versionRange;
            this.goals = goals;
        }
    }

    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        // TODO Auto-generated method stub

    }

    public void configureClasspath( IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor )
        throws CoreException
    {
        // TODO Auto-generated method stub

    }

    public void configureRawClasspath( ProjectConfigurationRequest request, IClasspathDescriptor classpath,
                                       IProgressMonitor monitor )
        throws CoreException
    {
        IMavenProjectFacade facade = request.getMavenProjectFacade();

        assertHasNature( request.getProject(), JavaCore.NATURE_ID );

        for ( MojoExecution mojoExecution : getExecutions( request, monitor ) )
        {
            if ( isSupportedExecution( mojoExecution ) )
            {
                File[] sources = getSourceFolders( request, mojoExecution );

                for ( File source : sources )
                {
                    IPath sourcePath = getFullPath( facade, source );

                    if ( sourcePath != null && !classpath.containsPath( sourcePath ) )
                    {
                        classpath.addSourceEntry( sourcePath, facade.getOutputLocation(), true );
                    }
                }
            }
        }
    }

    private List<MojoExecution> getExecutions( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        List<MojoExecution> executions = new ArrayList<MojoExecution>();
        List<Plugin> plugins = request.getMavenProjectFacade().getMavenProject( monitor ).getBuildPlugins();
        for ( Plugin plugin : plugins )
        {
            if ( plugin.getVersion() == null )
            {
                try
                {
                    String version =
                        maven.resolvePluginVersion( plugin.getGroupId(), plugin.getArtifactId(),
                                                    request.getMavenSession() );
                    plugin.setVersion( version );
                }
                catch ( CoreException ex )
                {
                    MavenLogger.log( ex );
                    console.logError( "Failed to determine plugin version for " + plugin );
                    continue;
                }
            }

            for ( PluginExecution execution : plugin.getExecutions() )
            {
                for ( String goal : execution.getGoals() )
                {
                    MojoExecution exec = new MojoExecution( plugin, goal, execution.getId() );
                    exec.setConfiguration( (Xpp3Dom) execution.getConfiguration() );
                    executions.add( exec );
                }
            }
        }
        return executions;
    }

    @Override
    public AbstractBuildParticipant getBuildParticipant( MojoExecution execution )
    {
        if ( isSupportedExecution( execution ) )
        {
            return doGetBuildParticipant( execution );
        }

        return null;
    }

    protected AbstractBuildParticipant doGetBuildParticipant( MojoExecution execution )
    {
        return null;
    }

    protected IPath getFullPath( IMavenProjectFacade facade, File file )
    {
        IProject project = facade.getProject();
        IPath path = MavenProjectUtils.getProjectRelativePath( project, file.getAbsolutePath() );
        return project.getFullPath().append( path );
    }

    public boolean isSupportedExecution( MojoExecution mojoExecution )
    {
        MojoExecutionKey executionKey = getMojoExecutionKey();

        return isSupportedExecution( mojoExecution, executionKey );
    }

    protected boolean isSupportedExecution( MojoExecution mojoExecution, MojoExecutionKey executionKey )
    {
        if ( !executionKey.groupId.equals( mojoExecution.getGroupId() )
            || !executionKey.artifactId.equals( mojoExecution.getArtifactId() ) )
        {
            return false;
        }

        VersionRange range;
        try
        {
            range = VersionRange.createFromVersionSpec( executionKey.version );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new IllegalStateException( "Can't parse version range", e );
        }
        DefaultArtifactVersion version = new DefaultArtifactVersion( mojoExecution.getVersion() );

        // XXX supported goal

        boolean supported = range.containsVersion( version );

        return supported;
    }

    protected File[] getSourceFolders( ProjectConfigurationRequest request, MojoExecution mojoExecution )
        throws CoreException
    {
        return new File[] { getParameterValue( getOutputFolderParameterName(), File.class, request.getMavenSession(),
                                               mojoExecution ) };
    }

    protected String getOutputFolderParameterName()
    {
        return "outputDirectory";
    }

    protected abstract MojoExecutionKey getMojoExecutionKey();

}

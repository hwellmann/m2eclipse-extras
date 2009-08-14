/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.mojos.internal;

import java.io.File;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IJavaProjectConfigurator;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectUtils;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

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

        for ( MojoExecution mojoExecution : facade.getExecutionPlan( monitor ).getExecutions() )
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

    protected boolean isSupportedExecution( MojoExecution mojoExecution )
    {
        MojoExecutionKey executionKey = getMojoExecutionKey();

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

        boolean supported = executionKey.groupId.equals( mojoExecution.getGroupId() ) //
            && executionKey.artifactId.equals( mojoExecution.getArtifactId() ) //
            && range.containsVersion( version );

        return supported;
    }

    protected File[] getSourceFolders( ProjectConfigurationRequest request, MojoExecution mojoExecution ) throws CoreException
    {
        return new File[] {getParameterValue( request.getMavenSession(), mojoExecution, getOutputFolderParameterName(), File.class )};
    }

    protected String getOutputFolderParameterName()
    {
        return "outputDirectory";
    }

    protected abstract MojoExecutionKey getMojoExecutionKey();

}

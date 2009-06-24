/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.buildhelper;

import java.io.File;

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
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

public class BuildhelperProjectConfigurator
    extends AbstractProjectConfigurator
    implements IJavaProjectConfigurator
{

    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
    }

    private boolean isBuildhelperExecution( MojoExecution mojoExecution )
    {
        return "org.codehaus.mojo".equals( mojoExecution.getGroupId() ) //
                && "build-helper-maven-plugin".equals( mojoExecution.getArtifactId() );
    }

    private IPath getFullPath( IMavenProjectFacade facade, File file )
    {
        IProject project = facade.getProject();
        IPath path = MavenProjectUtils.getProjectRelativePath( project, file.getAbsolutePath() );
        return project.getFullPath().append( path );
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
            if ( isBuildhelperExecution( mojoExecution ) )
            {
                File[] sources = getParameterValue( request.getMavenSession(), mojoExecution, "sources", File[].class );
                for ( File source : sources )
                {
                    IPath sourcePath = getFullPath( facade, source );

                    if ( sourcePath != null && !classpath.containsPath( sourcePath ) )
                    {
                        classpath.addSourceEntry( sourcePath, facade.getOutputLocation(), false );
                    }
                }
            }
        }
    }
    
}

/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.enforcer.internal;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;
import org.eclipse.m2e.jdt.IClasspathDescriptor;

public class EnforcerProjectConfigurator
    extends AbstractJavaProjectConfigurator
{
    @Override
    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade,
                                                         MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        return new EnforcerBuildParticipant( execution );
    }
    
    /*
     * I'm not sure why I'm getting a NullPointerException if I use AbstractJavaProjectConfigurator's configureRawClasspath method.
     * If I can figure that out then I don't need this section.
     * (non-Javadoc)
     * @see org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator#configureRawClasspath(org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest, org.eclipse.m2e.jdt.IClasspathDescriptor, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void configureRawClasspath( ProjectConfigurationRequest request, IClasspathDescriptor classpath,
            IProgressMonitor monitor )
    	throws CoreException
    {
    	IMavenProjectFacade facade = request.getMavenProjectFacade();

    	assertHasNature( request.getProject(), JavaCore.NATURE_ID );

    	for ( MojoExecution mojoExecution : getMojoExecutions( request, monitor ) )
    	{
    		File[] sources = getSourceFolders( request, mojoExecution );
    		
   			for ( File source : sources )
   			{
   	    		//Avoid NullPointerException
   				IPath sourcePath = null;
   				if(source != null) 
   				{
   					sourcePath = getFullPath( facade, source );
   				}
   					
   				if ( sourcePath != null && !classpath.containsPath( sourcePath ) )
    			{
    				classpath.addSourceEntry( sourcePath, facade.getOutputLocation(), true );
    			}    			
    		}
    	}
    }
}

/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.modello.internal;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelloBuildParticipant
    extends MojoExecutionBuildParticipant
{
    private static final Logger log = LoggerFactory.getLogger( ModelloBuildParticipant.class );

    private final ModelloProjectConfigurator projectConfigurator;

    public ModelloBuildParticipant( MojoExecution execution, ModelloProjectConfigurator projectConfigurator )
    {
        super( execution, true );
        this.projectConfigurator = projectConfigurator;
    }

    @Override
    public void clean( IProgressMonitor monitor )
        throws CoreException
    {
        super.clean( monitor );

        File[] outputFolders = projectConfigurator.getOutputFolders( getSession(), getMojoExecution() );
        for ( File outputFolder : outputFolders )
        {
            log.debug( "Deleting directory {}", outputFolder.getAbsolutePath() );
            if ( !outputFolder.exists() )
            {
                log.debug( "Directory {} does not exist", outputFolder.getAbsolutePath() );
                continue;
            }

            IProject project = getMavenProjectFacade().getProject();
            IFolder iOutputFolder = project.getFolder( getProjectRelativePath( project, outputFolder ) );
            if ( iOutputFolder != null )
            {
                iOutputFolder.delete( true /* force */, monitor );
            }
        }
    }

    public static IPath getProjectRelativePath( IProject project, File file )
    {
        IPath projectPath = project.getLocation();
        IPath filePath = new Path( file.getAbsolutePath() );
        if ( !projectPath.isPrefixOf( filePath ) )
        {
            return null;
        }

        return filePath.removeFirstSegments( projectPath.segmentCount() );
    }
}

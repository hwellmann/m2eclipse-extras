/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.modello.internal;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.configurator.MojoExecutionBuildParticipant;

public class ModelloBuildParticipant
    extends MojoExecutionBuildParticipant
{

    public ModelloBuildParticipant( MojoExecution execution )
    {
        super( execution, true );
    }

    @Override
    public void clean( IProgressMonitor monitor )
        throws CoreException
    {
//        IMavenProjectFacade facade = getMavenProjectFacade();
//
//        File gensrcFolder =
//            getParameterValue( getSession(), getMojoExecution(), "outputDirectory", File.class );
//        IPath gensrctPath = facade.getFullPath( gensrcFolder );
//
//        // TODO Auto-generated method stub
//        super.clean( monitor );
    }
}

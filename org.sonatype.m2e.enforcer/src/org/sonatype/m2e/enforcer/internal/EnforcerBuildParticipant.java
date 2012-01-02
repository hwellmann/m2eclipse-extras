/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.enforcer.internal;

import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;

public class EnforcerBuildParticipant
    extends MojoExecutionBuildParticipant
{

    public EnforcerBuildParticipant( MojoExecution execution )
    {
        super( execution, true );
    }

    @Override
    public Set<IProject> build( int kind, IProgressMonitor monitor )
        throws Exception
    {
        // execute mojo
        Set<IProject> result = super.build( kind, monitor );

        return result;
    }
}

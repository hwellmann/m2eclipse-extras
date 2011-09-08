/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.modello.internal;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;

public class ModelloProjectConfigurator
    extends AbstractJavaProjectConfigurator
{
    @Override
    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade, MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        return new ModelloBuildParticipant( execution, this );
    }

    File[] getOutputFolders( MavenSession mavenSession, MojoExecution mojoExecution )
        throws CoreException
    {
        return new File[] { getParameterValue( getOutputFolderParameterName(), File.class, mavenSession, mojoExecution ) };
    }
}

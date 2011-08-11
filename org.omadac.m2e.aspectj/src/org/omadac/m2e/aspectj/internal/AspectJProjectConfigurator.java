/*******************************************************************************
 * Copyright (c) 2011 Harald Wellmann
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.omadac.m2e.aspectj.internal;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;

public class AspectJProjectConfigurator extends AbstractJavaProjectConfigurator {

    @Override
    public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade,
            MojoExecution execution, IPluginExecutionMetadata executionMetadata) {
        return new AspectJBuildParticipant(execution);
    }

    /**
     * The aspectj-maven-plugin does not add any source folders.
     */
    protected File[] getSourceFolders(ProjectConfigurationRequest request,
            MojoExecution mojoExecution) throws CoreException {
        return new File[0];
    }
}

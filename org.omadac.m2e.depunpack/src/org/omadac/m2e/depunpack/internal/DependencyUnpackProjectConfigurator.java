/*******************************************************************************
 * Copyright (c) 2011 Martin Goldhahn
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.omadac.m2e.depunpack.internal;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;

public class DependencyUnpackProjectConfigurator extends AbstractJavaProjectConfigurator {
    
    public static final String OUTPUT_FOLDER_PARAMETER_NAME = "outputDirectory";

	@Override
    public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade,
            MojoExecution execution, IPluginExecutionMetadata executionMetadata) {
        return new DependencyUnpackBuildParticipant(execution);
    }

    @Override
    protected String getOutputFolderParameterName() {
        return OUTPUT_FOLDER_PARAMETER_NAME;
    }
}

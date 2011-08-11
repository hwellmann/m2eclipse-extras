/*******************************************************************************
 * Copyright (c) 2011 Harald Wellmann
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.omadac.m2e.aspectj.internal;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class AspectJBuildParticipant extends MojoExecutionBuildParticipant {

    public AspectJBuildParticipant(MojoExecution execution) {
        super(execution, true);
    }

    @Override
    public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {

        BuildContext buildContext = getBuildContext();

        // execute mojo
        Set<IProject> result = super.build(kind, monitor);

        // refresh class folders
        String outputDir = getSession().getCurrentProject().getBuild().getOutputDirectory();
        buildContext.refresh(new File(outputDir));

        return result;
    }
}

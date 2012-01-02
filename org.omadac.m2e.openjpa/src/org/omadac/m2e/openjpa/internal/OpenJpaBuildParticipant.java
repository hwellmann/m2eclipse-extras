/*******************************************************************************
 * Copyright (c) 2011 Harald Wellmann
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.omadac.m2e.openjpa.internal;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.Scanner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class OpenJpaBuildParticipant extends MojoExecutionBuildParticipant {

    public OpenJpaBuildParticipant(MojoExecution execution) {
        super(execution, true);
    }

    @Override
    public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        BuildContext buildContext = getBuildContext();

        if (hasEntityChange(buildContext)) {
            // execute mojo
            Set<IProject> result = super.build(kind, monitor);

            // refresh class folder
            File classesDir = getClassesDir();
            buildContext.refresh(classesDir);

            return result;

        } else {
            return Collections.emptySet();
        }
    }

    private boolean hasEntityChange(BuildContext buildContext) throws CoreException {
        IMaven maven = MavenPlugin.getMaven();

        File classesDir = getClassesDir();
        Scanner scanner = buildContext.newScanner(classesDir);
        String includes = maven.getMojoParameterValue(getSession(), getMojoExecution(), "includes", String.class);
        scanner.setIncludes(splitParameters(includes));
        String excludes = maven.getMojoParameterValue(getSession(), getMojoExecution(), "excludes", String.class);
        scanner.setExcludes(splitParameters(excludes));

        scanner.scan();
        String[] includedFiles = scanner.getIncludedFiles();
        if (includedFiles == null || includedFiles.length == 0) {
            return false;

        } else {
            return true;
        }
    }

    private File getClassesDir() throws CoreException {
        IMaven maven = MavenPlugin.getMaven();
        return maven.getMojoParameterValue(getSession(), getMojoExecution(), "classes", File.class);
    }

    private String[] splitParameters(String parameters) {
        String[] result;
        if (parameters == null) {
            result = new String[0];

        } else {
            result = parameters.split(",");
        }

        return result;
    }
}

/*******************************************************************************
 * Copyright (c) 2011 Martin Goldhahn
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.omadac.m2e.depunpack.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.Scanner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class DependencyUnpackBuildParticipant extends MojoExecutionBuildParticipant {

    public DependencyUnpackBuildParticipant(MojoExecution execution) {
        super(execution, true);
    }

    @Override
    public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        IMaven maven = MavenPlugin.getMaven();
        BuildContext buildContext = getBuildContext();

        // Since the artifacts are fetched from a remote repository,
        // we check only if there are any files in the output directory

        File generatedDir = maven.getMojoParameterValue(getSession(), getMojoExecution(),
                DependencyUnpackProjectConfigurator.OUTPUT_FOLDER_PARAMETER_NAME, File.class);
        
        String[] fileList = generatedDir.list(FilterHolder.filter);
        if (fileList != null && fileList.length > 0) {
        	// there are some files, we assume the mojo was run previously
        	return null;
        }
        
        // execute mojo
        Set<IProject> result = super.build(kind, monitor);

        buildContext.refresh(generatedDir);

        return result;
    }
    
    private static FilenameFilter createFileFilter() {
        return new FilenameFilter() {

    			public boolean accept(File dir, String name) {
    				if (name.startsWith(".")) {
    					return false;
    				}
    				return true;
    			}
    		};
    }
    
    private static class FilterHolder {
    	private static FilenameFilter filter = createFileFilter();

    }
}

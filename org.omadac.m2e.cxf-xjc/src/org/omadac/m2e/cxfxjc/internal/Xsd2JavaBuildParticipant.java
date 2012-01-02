/*******************************************************************************
 * Copyright (c) 2011 Martin Goldhahn
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.omadac.m2e.cxfxjc.internal;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class Xsd2JavaBuildParticipant extends MojoExecutionBuildParticipant {

	public Xsd2JavaBuildParticipant(MojoExecution execution) {
        super(execution, true);
    }

    @Override
    public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        IMaven maven = MavenPlugin.getMaven();
        BuildContext buildContext = getBuildContext();

        // check if any of the schema files changed
        List<?> xsdOptions = maven.getMojoParameterValue(getSession(), getMojoExecution(),
                "xsdOptions", List.class);

        boolean needBuild = false;
        File sourceRoot = maven.getMojoParameterValue(getSession(), getMojoExecution(),
                "sourceRoot", File.class);
        
        for (Object xsdOptionObj : xsdOptions) {
			XsdOptionWrapper xsdOption = new XsdOptionWrapper(xsdOptionObj);
			String packName = xsdOption.getPackageName();
			String xsd = xsdOption.getXsd();
			
			// if modified xsd
			needBuild = xsdFileModified(sourceRoot, packName, xsd);
			if (needBuild) {
				break;
			}
		}

        if (!needBuild) {
        	return null;
        }
        
        // execute mojo
        Set<IProject> result = super.build(kind, monitor);

        // tell m2e builder to refresh generated files
        if (sourceRoot != null) {
            buildContext.refresh(sourceRoot);
        }

        return result;
    }
    
    private boolean xsdFileModified(File sourceRoot, String packName, String xsd) {
    	
    	File targetPath = new File(sourceRoot, packName.replace('.', File.separatorChar));
    	
    	// pick the first file in the directory and compare the modification date
    	File[] targetFiles = targetPath.listFiles();
    	if (targetFiles == null || targetFiles.length == 0) {
    		return true;
    	}
    	File xsdFile = new File(xsd);
    	
    	return (targetFiles[0].lastModified() < xsdFile.lastModified());
	}

	public static class XsdOptionWrapper {

    	final Object target;
    	final Method mGetPackageName;
    	final Method mGetXsd;
    	
		public XsdOptionWrapper(Object target) throws Exception {
			this.target = target;
			mGetPackageName = target.getClass().getMethod("getPackagename");
			mGetXsd = target.getClass().getMethod("getXsd");
		}

		public String getPackageName() {
			try {
				return (String) mGetPackageName.invoke(target);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

		public String getXsd() {
			try {
				return (String) mGetXsd.invoke(target);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

	}
}

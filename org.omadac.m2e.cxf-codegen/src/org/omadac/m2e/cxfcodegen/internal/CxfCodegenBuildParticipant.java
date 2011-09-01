/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.omadac.m2e.cxfcodegen.internal;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.maven_plugin.WsdlOption;
import org.apache.maven.plugin.MojoExecution;
import org.bitstrings.eclipse.m2e.common.BuildHelper;
import org.codehaus.plexus.util.Scanner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class CxfCodegenBuildParticipant extends MojoExecutionBuildParticipant {

    public CxfCodegenBuildParticipant(MojoExecution execution) {
        super(execution, true);
    }

    @Override
    public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
        IMaven maven = MavenPlugin.getMaven();
        BuildContext buildContext = getBuildContext();

        boolean needBuild = false;
        
    	// check if the WSDL files are modified
    	
    	// default option (http://cxf.apache.org/docs/maven-cxf-codegen-plugin-wsdl-to-java.html#Mavencxf-codegen-plugin%28WSDLtoJava%29-Example3:UsingdefaultOptiontoavoidrepetition)
    	WsdlOption defaultOptions = maven.getMojoParameterValue(getSession(), getMojoExecution(), "defaultOptions", WsdlOption.class);
    	needBuild = needBuild(buildContext, defaultOptions);
    	
    	if (!needBuild) {
        	// check wsdl if files have changed
			List<?> wsdlOptions = maven.getMojoParameterValue(getSession(), getMojoExecution(), "wsdlOptions", List.class);
        	for (Object wsdlOptionObj : wsdlOptions) {
        		WsdlOptionWrapper wsdlOption = new WsdlOptionWrapper(wsdlOptionObj);
				needBuild = needBuild(buildContext, wsdlOption);
				if (needBuild) {
					break;
				}
			}
    	}

        if (!needBuild) {
        	return null;
        }
        
		final Set<IProject> result = super.build(kind, monitor);

		File sourceRoot = maven.getMojoParameterValue(getSession(), getMojoExecution(),
                "sourceRoot", File.class);
		if (sourceRoot != null) {
			buildContext.refresh(sourceRoot);
		}

		return result;
    }

	private boolean needBuild(BuildContext buildContext, WsdlOption wsdlOption)
			throws Exception {
		boolean needBuild = false;
		String[] defaultBindingFiles = wsdlOption.getBindingFiles();

		String defaultWsdl = wsdlOption.getWsdl();
		needBuild = (!StringUtils.isEmpty(defaultWsdl) && !ArrayUtils
				.isEmpty(BuildHelper.getModifiedFiles(buildContext,
						new File(defaultWsdl))));
		
		if (needBuild) {
			return true;
		}
		
		for (String bindingFile : defaultBindingFiles) {
			needBuild = (!StringUtils.isEmpty(bindingFile) && !ArrayUtils
					.isEmpty(BuildHelper.getModifiedFiles(
							buildContext, new File(bindingFile))));

			if (needBuild) {
				return true;
			}
		}
		
		return false;
	}
	
	// a class to cope with the class loading issues
	private static class WsdlOptionWrapper extends WsdlOption {
		private final Object target;
		
		private final Method mGetWsdl;
		private final Method mGetBindingFiles;
		
		public WsdlOptionWrapper(Object target) throws Exception {
			this.target = target;
			mGetWsdl = target.getClass().getMethod("getWsdl");
			mGetBindingFiles = target.getClass().getMethod("getBindingFiles");
		}

		@Override
		public String getWsdl() {
			try {
				return (String) mGetWsdl.invoke(target);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String[] getBindingFiles() {
			try {
				return (String[]) mGetBindingFiles.invoke(target);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}

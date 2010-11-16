/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.buildhelper;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.maven.ide.eclipse.mojos.internal.AbstractJavaProjectConfigurator;

public class BuildhelperProjectConfigurator
    extends AbstractJavaProjectConfigurator
{
    @Override
    protected MojoExecutionKey getMojoExecutionKey()
    {
        return new MojoExecutionKey( "org.codehaus.mojo", "build-helper-maven-plugin", "[1.0,)", "add-source,add-test-source" );
    }

    @Override
    protected File[] getSourceFolders( ProjectConfigurationRequest request, MojoExecution mojoExecution )
        throws CoreException
    {
        return getParameterValue( "sources", File[].class, request.getMavenSession(), mojoExecution );
    }
}

/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.antlr.internal;

import org.apache.maven.plugin.MojoExecution;
import org.maven.ide.eclipse.mojos.internal.AbstractJavaProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;

public class AntlrProjectConfigurator
    extends AbstractJavaProjectConfigurator
{

    private static final MojoExecutionKey EXECUTION_KEY = new MojoExecutionKey( "org.codehaus.mojo", "antlr-maven-plugin", "[2.1,)", "generate" );

    @Override
    protected MojoExecutionKey getMojoExecutionKey()
    {
        return EXECUTION_KEY;
    }

    @Override
    public AbstractBuildParticipant doGetBuildParticipant( MojoExecution execution )
    {
        return new AntlrBuildParticipant( execution );
    }

}

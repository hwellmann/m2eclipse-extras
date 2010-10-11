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

    private static final MojoExecutionKey EXECUTION_KEY = new MojoExecutionKey( "org.codehaus.mojo",
                                                                                "antlr-maven-plugin", "[2.1,)",
                                                                                "generate" );

    private static final MojoExecutionKey EXECUTION_KEY_V3 = new MojoExecutionKey( "org.antlr", "antlr3-maven-plugin",
                                                                                   "[3.1.1,)", "generate" );

    @Override
    protected MojoExecutionKey getMojoExecutionKey()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isSupportedExecution( MojoExecution mojoExecution )
    {
        return isSupportedExecution( mojoExecution, EXECUTION_KEY )
            || isSupportedExecution( mojoExecution, EXECUTION_KEY_V3 );
    }

    @Override
    public AbstractBuildParticipant doGetBuildParticipant( MojoExecution execution )
    {
        return new AntlrBuildParticipant( execution );
    }

}

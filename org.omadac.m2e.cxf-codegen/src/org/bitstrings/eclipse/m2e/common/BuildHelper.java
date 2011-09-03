/*
 *
 *    Copyright (c) 2011 bitstrings.org - Pino Silvaggio
 *
 *    All rights reserved. This program and the accompanying materials
 *    are made available under the terms of the Eclipse Public License v1.0
 *    which accompanies this distribution, and is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.bitstrings.eclipse.m2e.common;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.Scanner;
import org.eclipse.m2e.core.embedder.IMaven;
import org.sonatype.plexus.build.incremental.BuildContext;

public final class BuildHelper
{
    private BuildHelper() {}

    public static String[] getModifiedFiles(
                                    MavenSession mavenSession, MojoExecution mojoExecution,
                                    IMaven maven, BuildContext buildContext,
                                    File source,
                                    String includesParam, String excludesParam)
        throws Exception
    {
        return getModifiedFiles(buildContext,
                        source,
                        maven.getMojoParameterValue(mavenSession, mojoExecution, includesParam, String[].class),
                        maven.getMojoParameterValue(mavenSession, mojoExecution, excludesParam, String[].class));
    }

    public static String[] getModifiedFiles(
                                    MavenSession mavenSession, MojoExecution mojoExecution,
                                    IMaven maven, BuildContext buildContext,
                                    String sourceParam,
                                    String includesParam, String excludesParam)
        throws Exception
    {
        return
            getModifiedFiles(
                    buildContext,
                    maven.getMojoParameterValue(mavenSession, mojoExecution, sourceParam, File.class),
                    maven.getMojoParameterValue(mavenSession, mojoExecution, includesParam, String[].class),
                    maven.getMojoParameterValue(mavenSession, mojoExecution, excludesParam, String[].class));
    }

    public static String[] getModifiedFiles(
                                    MavenSession mavenSession, MojoExecution mojoExecution,
                                    IMaven maven, BuildContext buildContext,
                                    String sourceParam,
                                    String[] includes, String[] excludes)
        throws Exception
    {
        return
            getModifiedFiles(
                    buildContext,
                    maven.getMojoParameterValue(mavenSession, mojoExecution, sourceParam, File.class),
                    includes,
                    excludes);
    }

    public static String[] getModifiedFiles(
                                    BuildContext buildContext,
                                    File source,
                                    String[] includes, String[] excludes)
        throws Exception
    {
        if ((source == null) || !source.exists())
        {
            return null;
        }

        final Scanner ds = buildContext.newScanner(source);

        if ((includes != null) && (includes.length > 0))
        {
            ds.setIncludes(includes);
        }

        if ((excludes != null) && (excludes.length > 0))
        {
            ds.setExcludes(excludes);
        }

        ds.scan();

        return ds.getIncludedFiles();
    }

    public static String[] getModifiedFiles(BuildContext buildContext, File source)
        throws Exception
    {
        return getModifiedFiles(buildContext, source, null, null);
    }

    public static String[] getModifiedFiles(
                                    MavenSession mavenSession, MojoExecution mojoExecution,
                                    IMaven maven, BuildContext buildContext,
                                    String sourceParam)
        throws Exception
    {
        return
            getModifiedFiles(
                    buildContext,
                    maven.getMojoParameterValue(mavenSession, mojoExecution, sourceParam, File.class),
                    null,
                    null);
    }
}

/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.plexus.annotations.internal;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class PlexusProjectConfigurator
    extends AbstractProjectConfigurator
{

    private static final String PLEXUS_GROUP_ID = "org.codehaus.plexus";

    private static final String PLEXUS_ARTIFACT_ID = "plexus-component-metadata";

    @Override
    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade, MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        if ( !isPlexusExecution( execution ) )
        {
            throw new IllegalArgumentException();
        }
        if ( "generate-metadata".equals( execution.getGoal() ) )
        {
            return new PlexusBuildParticipant( false );
        }
        else if ( "generate-test-metadata".equals( execution.getGoal() ) )
        {
            return new PlexusBuildParticipant( true );
        }
        return null;
    }

    private boolean isPlexusExecution( MojoExecution execution )
    {
        return PLEXUS_GROUP_ID.equals( execution.getGroupId() ) //
            && PLEXUS_ARTIFACT_ID.equals( execution.getArtifactId() );
    }

    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        // nothing to do
    }

    @Override
    public void mavenProjectChanged( MavenProjectChangedEvent event, IProgressMonitor monitor )
        throws CoreException
    {
        if ( MavenProjectChangedEvent.KIND_REMOVED == event.getKind() )
        {
            PlexusMetadata metadata = PlexusBuildParticipant.getPlexusMetadata();

            metadata.clean( event.getOldMavenProject().getProject() );
        }
    }
}

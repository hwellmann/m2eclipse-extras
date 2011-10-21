package org.sonatype.m2e.sisuindex.internal;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class SisuIndexProjectConfigurator
    extends AbstractProjectConfigurator
{
    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
    }

    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade, MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        return new MojoExecutionBuildParticipant( execution, false )
        {
            @Override
            public Set<IProject> build( int kind, IProgressMonitor monitor )
                throws Exception
            {
                File index = getIndexFile();

                if ( index == null )
                {
                    return Collections.emptySet();
                }

                Set<IProject> projects = super.build( kind, monitor );

                getBuildContext().refresh( index );

                return projects;
            }

            @Override
            public boolean appliesToBuildKind( int kind )
            {
                File indexFile = getIndexFile();

                if ( indexFile == null )
                {
                    return false;
                }

                return super.appliesToBuildKind( kind ) || !indexFile.canRead();
            }

            protected File getIndexFile()
            {
                MavenProject mavenProject = getMavenProjectFacade().getMavenProject();

                String outputdir = null;

                String goal = getMojoExecution().getGoal();
                if ( "main-index".equals( goal ) )
                {
                    outputdir = mavenProject.getBuild().getOutputDirectory();
                }
                else if ( "test-index".equals( goal ) )
                {
                    outputdir = mavenProject.getBuild().getTestOutputDirectory();
                }

                if ( outputdir == null )
                {
                    return null;
                }

                return new File( outputdir, "META-INF/sisu/javax.inject.Named" );
            }
        };
    };
}

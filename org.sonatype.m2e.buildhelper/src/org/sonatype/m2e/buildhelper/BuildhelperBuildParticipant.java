package org.sonatype.m2e.buildhelper;

import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.sonatype.m2e.buildhelper.mojohandler.BuildhelperMojoHandler;

/**
 * {@link AbstractBuildParticipant} used to support the <tt>Build Helper Maven Plugin</tt>. This class acts as a
 * mediator delegating the appropriate {@link BuildhelperMojoHandler}s.
 * 
 * @see BuildhelperMojoHandler
 * @author Phillip Webb
 */
public class BuildhelperBuildParticipant
    extends AbstractBuildParticipant
{
    private List<BuildhelperMojoHandler> handlers;

    private MojoExecution execution;

    public BuildhelperBuildParticipant( List<BuildhelperMojoHandler> handlers, MojoExecution execution )
    {
        this.handlers = handlers;
        this.execution = execution;
    }

    @Override
    public Set<IProject> build( int kind, IProgressMonitor monitor )
        throws Exception
    {
        for ( BuildhelperMojoHandler handler : handlers )
        {
            if ( handler.supports( execution ) )
            {
                BuildContext context = new BuildContextImpl( kind, monitor );
                handler.build( context );
            }
        }
        return null;
    }

    private class BuildContextImpl
        implements BuildContext
    {
        private int kind;

        private IProgressMonitor monitor;

        public BuildContextImpl( int kind, IProgressMonitor monitor )
        {
            this.kind = kind;
            this.monitor = monitor;
        }

        public void executeMojo()
        {
            IMaven maven = MavenPlugin.getMaven();
            maven.execute( getSession(), execution, monitor );
        }

        public int getKind()
        {
            return kind;
        }

        public IProgressMonitor getMonitor()
        {
            return monitor;
        }
    }
}

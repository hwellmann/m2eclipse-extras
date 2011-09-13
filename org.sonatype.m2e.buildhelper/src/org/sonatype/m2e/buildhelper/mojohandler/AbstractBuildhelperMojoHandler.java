package org.sonatype.m2e.buildhelper.mojohandler;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.sonatype.m2e.buildhelper.BuildContext;
import org.sonatype.m2e.buildhelper.ConfigureRawClasspathContext;

/**
 * Convenient base class for {@link BuildhelperMojoHandler}s that are tied to a single MOJO <tt>goal</tt>.
 * 
 * @author Phillip Webb
 */
public abstract class AbstractBuildhelperMojoHandler
    implements BuildhelperMojoHandler
{
    /**
     * Rreturns the MOJO goal that the handler supports.
     * 
     * @return the goal
     */
    protected abstract String getGoal();

    public boolean supports( MojoExecution execution )
    {
        return execution.getGoal().equals( getGoal() );
    }

    public void configureRawClasspath( ConfigureRawClasspathContext context )
        throws CoreException
    {
    }

    public void build( BuildContext context )
    {
    }
}

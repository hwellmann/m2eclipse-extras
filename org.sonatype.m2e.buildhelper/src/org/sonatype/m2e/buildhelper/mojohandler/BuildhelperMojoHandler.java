package org.sonatype.m2e.buildhelper.mojohandler;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.sonatype.m2e.buildhelper.BuildContext;
import org.sonatype.m2e.buildhelper.BuildhelperProjectConfigurator;
import org.sonatype.m2e.buildhelper.ConfigureRawClasspathContext;

/**
 * A handler class used by {@link BuildhelperProjectConfigurator} to handle the actual work of configuring an
 * {@link MojoExecution executing} <tt>build helper</tt> MOJO.
 * 
 * @see AbstractBuildhelperMojoHandler
 * @author Phillip Webb
 */
public interface BuildhelperMojoHandler
{
    /**
     * Determine if the specific execution is handled by this class.
     * 
     * @param execution the execution
     * @return <tt>true</tt> if the execution is supported.
     */
    boolean supports( MojoExecution execution );

    /**
     * Configure the raw classpath using the specified context. This method is only called when {@link #supports}
     * returns <tt>true</tt>.
     * 
     * @param context the context
     * @throws CoreException
     */
    public void configureRawClasspath( ConfigureRawClasspathContext context )
        throws CoreException;

    /**
     * Execute a build using the specified context. This method is only called when {@link #supports} returns
     * <tt>true</tt>.
     * 
     * @param context the context
     * @throws CoreException
     */
    public void build( BuildContext context );
}

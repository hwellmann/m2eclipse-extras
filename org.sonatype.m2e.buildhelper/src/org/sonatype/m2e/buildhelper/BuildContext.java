package org.sonatype.m2e.buildhelper;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonatype.m2e.buildhelper.mojohandler.BuildhelperMojoHandler;

/**
 * Provides context for {@link BuildhelperMojoHandler#build(BuildContext)}.
 * 
 * @author Phillip Webb
 */
public interface BuildContext
{
    /**
     * Execute the running mojo.
     */
    void executeMojo();

    /**
     * Returns the kind of build that is running (see {@link IncrementalProjectBuilder} for definitions).
     * 
     * @return the kind of build
     */
    int getKind();

    /**
     * Returns the progress monitor.
     * 
     * @return the progress monitor
     */
    IProgressMonitor getMonitor();

}

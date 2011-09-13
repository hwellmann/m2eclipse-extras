package org.sonatype.m2e.buildhelper;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.sonatype.m2e.buildhelper.mojohandler.BuildhelperMojoHandler;

/**
 * Provides context for {@link BuildhelperMojoHandler#configureRawClasspath(ConfigureRawClasspathContext)}.
 * 
 * @author Phillip Webb
 */
public interface ConfigureRawClasspathContext
{
    /**
     * Returns a parameter value by inspecting the configuration of MOJO that is currently executing.
     * 
     * @param parameter the parameter to return
     * @param asType the return type expected
     * @return the parameter value
     * @throws CoreException
     */
    <T> T getParameterValue( String parameter, Class<T> asType )
        throws CoreException;

    /**
     * Returns the path of given file. For example if <tt>file</tt> is
     * <tt>/home/user/eclipseworkspace/project/src/main/java</tt> the returned path would be
     * <tt>[PROJECT]/src/main/java</tt>.
     * 
     * @param file the file
     * @return the path of the file (relative to the workspace).
     */
    IPath getPath( File file );

    /**
     * Returns the path of given POM relative location. For example if <tt>pomRelativeLocation</tt> is
     * <tt>src/main/java</tt> the returned path would be <tt>[PROJECT]/src/main/java</tt>.
     * 
     * @param pomRelativeLocation the location
     * @return the path of the location (relative to the workspace).
     */
    IPath getPath( String pomRelativeLocation );

    /**
     * Returns the classpath descriptor being configured.
     * 
     * @return the classpath descriptor.
     */
    IClasspathDescriptor getClasspath();

    /**
     * Returns the full, absolute path of this project maven build output directory relative to the workspace or null if
     * maven build output directory cannot be determined or is outside of the workspace.
     * 
     * @return the output location
     */
    IPath getOutputLocation();

    /**
     * Returns the full, absolute path of this project maven build test output directory relative to the workspace or
     * null if maven build output directory cannot be determined or is outside of the workspace.
     * 
     * @return the test output location
     */
    IPath getTestOutputLocation();

    /**
     * Returns the source project being configured.
     * 
     * @return the project
     */
    IProject getProject();

    /**
     * Returns the active {@link IProgressMonitor}.
     * 
     * @return the progress monitor
     */
    IProgressMonitor getMonitor();

    /**
     * Returns the encoding that should be used for resources or <tt>null</tt> if not specific encoding has been
     * defined.
     * 
     * @return the resource encoding
     */
    String getResourceEncoding();

    /**
     * Returns the encoding that should be used for test resources or <tt>null</tt> if not specific encoding has been
     * defined.
     * 
     * @return the test resource encoding
     */
    String getTestResourceEncoding();
}

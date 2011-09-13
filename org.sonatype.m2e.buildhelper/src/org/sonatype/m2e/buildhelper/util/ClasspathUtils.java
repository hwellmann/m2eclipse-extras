package org.sonatype.m2e.buildhelper.util;

import java.io.File;

import org.apache.maven.model.Resource;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.sonatype.m2e.buildhelper.ConfigureRawClasspathContext;

/**
 * General purpose utilities when working with {@link IClasspathDescriptor}s.
 * 
 * @author Igor Fedorenko
 * @author Phillip Webb
 */
public abstract class ClasspathUtils
{
    /**
     * Private constructor.
     */
    private ClasspathUtils()
    {
    }

    /**
     * Add source paths to the {@link ConfigureRawClasspathContext#getClasspath() classpath}.
     * 
     * @param context the context
     * @param projectRelativePaths the project relative paths to add.
     * @param outputLocation the output location for the paths
     * @throws CoreException
     */
    public static void addSourcePaths( ConfigureRawClasspathContext context, File[] projectRelativePaths,
                                       IPath outputLocation )
        throws CoreException
    {
        if ( outputLocation == null )
        {
            return;
        }
        IClasspathDescriptor classpath = context.getClasspath();
        for ( File projectRelativePath : projectRelativePaths )
        {
            IPath fullPath = context.getPath( projectRelativePath );
            if ( fullPath != null && !classpath.containsPath( fullPath ) )
            {
                classpath.addSourceEntry( fullPath, outputLocation, true );
            }
        }
    }

    /**
     * Add resources to the {@link ConfigureRawClasspathContext#getClasspath() classpath}.
     * 
     * @param context the context
     * @param resources the resources to add
     * @param outputLocation the output location for the resources
     * @param charset the charset of the resources
     * @throws CoreException
     */
    public static void addResources( ConfigureRawClasspathContext context, Resource[] resources, IPath outputLocation,
                                     String charset )
        throws CoreException
    {
        if ( outputLocation == null )
        {
            return;
        }
        IProject project = context.getProject();
        IClasspathDescriptor classpath = context.getClasspath();
        IProgressMonitor monitor = context.getMonitor();

        for ( Resource resource : resources )
        {
            IPath resourcePath = context.getPath( resource.getDirectory() );
            IFolder folder = project.getParent().getFolder( resourcePath );
            if ( ( folder != null ) && ( folder != project ) && ( folder.getProject().equals( project ) )
                && ( folder.exists() ) )
            {
                IClasspathEntryDescriptor classpathEntry = getClasspathEntry( classpath, folder.getFullPath() );
                if ( classpathEntry == null )
                {
                    classpath.addSourceEntry( folder.getFullPath(), outputLocation, new IPath[0],
                                              new IPath[] { new Path( "**" ) }, false );
                }
                else
                {
                    // resources and sources folders overlap. make sure JDT only processes java sources.
                    classpathEntry.addInclusionPattern( new Path( "**/*.java" ) );
                }

                // Set folder encoding (null = platform default)
                folder.setDefaultCharset( charset, monitor );
            }
        }
    }

    private static IClasspathEntryDescriptor getClasspathEntry( IClasspathDescriptor classpath, IPath fullPath )
    {
        for ( IClasspathEntryDescriptor classpathEntry : classpath.getEntryDescriptors() )
        {
            if ( classpathEntry.getPath().isPrefixOf( fullPath ) )
            {
                return classpathEntry;
            }
        }
        return null;
    }
}

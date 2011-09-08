/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.plexus.annotations.internal;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.jdt.IClasspathManager;

public class PlexusBuildParticipant
    extends AbstractBuildParticipant
{

    private static final JDTComponentGleaner gleaner = new JDTComponentGleaner();

    private static final int DELTA_KIND = IResourceDelta.ADDED | IResourceDelta.REMOVED | IResourceDelta.REPLACED
        | IResourceDelta.CHANGED;

    private static final int DELTA_FLAGS = IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
        | IResourceDelta.COPIED_FROM | IResourceDelta.REPLACED;

    private final boolean testMetadata;

    public PlexusBuildParticipant( boolean testMetadata )
    {
        this.testMetadata = testMetadata;
    }

    @Override
    public boolean callOnEmptyDelta()
    {
        return true;
    }

    @Override
    public Set<IProject> build( int kind, final IProgressMonitor monitor )
        throws CoreException
    {
        final IProject project = getMavenProjectFacade().getProject();
        final IJavaProject javaProject = JavaCore.create( project );

        if ( !javaProject.exists() )
        {
            return null;
        }

        boolean changed = false;

        final PlexusMetadata metadata = getPlexusMetadata();

        final Set<IResource> processed = new HashSet<IResource>();

        for ( IClasspathEntry cpe : javaProject.getRawClasspath() )
        {
            if ( cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE && isTestEntry( cpe ) == testMetadata )
            {
                changed = true; // XXX not specific enough!

                IPath path = cpe.getPath().removeFirstSegments( 1 );
                if ( IncrementalProjectBuilder.FULL_BUILD == kind || metadata.isFullBuildRequired( project ) )
                {
                    IFolder folder = project.getFolder( path );
                    if ( !folder.exists() )
                    {
                        // optional source folders may not exist
                        continue;
                    }
                    folder.accept( new IResourceVisitor()
                    {
                        public boolean visit( IResource resource )
                            throws CoreException
                        {
                            if ( !javaProject.isOnClasspath( resource ) )
                            {
                                return false;
                            }
                            if ( resource instanceof IFile )
                            {
                                IFile file = (IFile) resource;
                                processAnnotations( metadata, javaProject, file, processed, monitor );
                            }
                            return true; // keep visiting
                        }
                    } );
                }
                else
                {
                    IResourceDelta delta = getDelta( project ).findMember( path );

                    if ( delta == null )
                    {
                        continue;
                    }

                    changed = true; // XXX not specific enough!

                    delta.accept( new IResourceDeltaVisitor()
                    {
                        public boolean visit( IResourceDelta delta )
                            throws CoreException
                        {
                            IResource resource = delta.getResource();
                            if ( !javaProject.isOnClasspath( resource ) )
                            {
                                return false;
                            }
                            if ( resource instanceof IFile && isInteresting( delta ) )
                            {
                                IFile file = (IFile) resource;
                                if ( file.isAccessible() )
                                {
                                    processAnnotations( metadata, javaProject, file, processed, monitor );
                                }
                                else
                                {
                                    metadata.remove( file );
                                }
                            }
                            return true; // keep visiting
                        }
                    } );
                }
            }
        }

        List<IResource> staleResources = metadata.getStaleResources( project, testMetadata );
        changed |= !staleResources.isEmpty();

        do
        {
            for ( IResource resource : staleResources )
            {
                if ( resource instanceof IFile )
                {
                    processAnnotations( metadata, javaProject, (IFile) resource, processed, monitor );
                }
            }
            staleResources = metadata.getStaleResources( project, testMetadata );
            // this is actually a bug, but we report it later
            staleResources.removeAll( processed );
        }
        while ( !staleResources.isEmpty() );

        if ( changed )
        {
            Set<IProject> dependencies = metadata.writeMetadata( project, testMetadata );
            return dependencies;
        }

        return null;
    }

    private static PlexusMetadata metadata;

    static synchronized PlexusMetadata getPlexusMetadata()
    {
        if ( metadata == null )
        {
            metadata = new PlexusMetadata();
        }
        return metadata;
    }

    private boolean isTestEntry( IClasspathEntry cpe )
    {
        IClasspathAttribute[] attrs = cpe.getExtraAttributes();
        if ( attrs != null )
        {
            for ( IClasspathAttribute attr : attrs )
            {
                if ( IClasspathManager.SCOPE_ATTRIBUTE.equals( attr.getName() ) )
                {
                    return Artifact.SCOPE_TEST.equals( attr.getValue() );
                }
            }
        }

        if ( cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE )
        {
            IProject project = getMavenProjectFacade().getProject();
            for ( IPath testPath : getMavenProjectFacade().getTestCompileSourceLocations() )
            {
                if ( project.getFolder( testPath ).getFullPath().equals( cpe.getPath() ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isInteresting( IResourceDelta delta )
    {
        return ( ( delta.getKind() & DELTA_KIND ) != 0 ) && ( ( delta.getFlags() & DELTA_FLAGS ) != 0 );
    }

    protected void processAnnotations( PlexusMetadata metadata, IJavaProject javaProject, IFile file,
                                       Set<IResource> processed, IProgressMonitor monitor )
        throws JavaModelException
    {

        if ( processed.contains( file ) )
        {
            return;
        }

        if ( !javaProject.getProject().equals( file.getProject() ) )
        {
            throw new IllegalArgumentException();
        }

        ICompilationUnit cu = null;
        try
        {
            cu = JavaCore.createCompilationUnitFrom( file );
        }
        catch ( IllegalArgumentException e )
        {
            // JavaCore.createCompilationUnitFrom does not like non-java files
            // apparently in violation of API spec...
        }
        if ( cu != null )
        {
            metadata.invalidateMetadata( new HashSet<IResource>( processed ), file );

            for ( IType type : cu.getTypes() )
            {
                if ( Flags.isPublic( type.getFlags() ) )
                {
                    ComponentDescriptor descriptor = gleaner.glean( type, monitor );
                    if ( descriptor != null )
                    {
                        metadata.addDescriptor( file, getResources( type ), descriptor, testMetadata );
                    }
                    else
                    {
                        metadata.remove( file );
                    }
                }
            }
        }

        processed.add( file );
    }

    private Set<IResource> getResources( IType type )
        throws JavaModelException
    {
        LinkedHashSet<IResource> resources = new LinkedHashSet<IResource>();

        // TODO should we add implemented interfaces too?

        ITypeHierarchy hierarchy = type.newSupertypeHierarchy( new NullProgressMonitor() );
        IType curr = type;
        while ( curr != null )
        {
            if ( !curr.isBinary() )
            {
                resources.add( curr.getResource() );
            }
            curr = hierarchy.getSuperclass( curr );
        }

        return resources;
    }

    @Override
    public void clean( IProgressMonitor monitor )
        throws CoreException
    {
        getPlexusMetadata().clean( getMavenProjectFacade().getProject() );
    }
}

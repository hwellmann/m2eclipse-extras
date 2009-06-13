package org.maven.ide.eclipse.modello.internal;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.jdt.AbstractJavaProjectConfigurator;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectUtils;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.MojoExecutionBuildParticipant;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
import org.maven.ide.eclipse.util.Util;

public class ModelloProjectConfigurator
    extends AbstractJavaProjectConfigurator
{
    private static final String MODELLO_GROUP_ID = "org.codehaus.modello";

    private static final String MODELLO_ARTIFACT_ID = "modello-maven-plugin";

    private static final String MODELLO_VERSION_RANGE = "[1.0.1,)";

    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        IProject project = request.getProject();
        IMavenProjectFacade facade = request.getMavenProjectFacade();

        assertHasNature( project, JavaCore.NATURE_ID );

        IJavaProject javaProject = JavaCore.create( project );

        Map<IPath, IClasspathEntry> cp = getRawClasspath( javaProject );

        for ( MojoExecution mojoExecution : facade.getExecutionPlan( monitor ).getExecutions() )
        {
            if ( isModelloExecution( mojoExecution ) )
            {
                File gensrcFolder =
                    getParameterValue( request.getMavenSession(), mojoExecution, "outputDirectory", File.class );
                IPath gensrctPath = getFullPath( facade, gensrcFolder );

                if ( gensrctPath != null && !cp.containsKey( gensrctPath ) )
                {
                    IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();

                    Util.createFolder( workspaceRoot.getFolder( gensrctPath ), true );

                    IClasspathAttribute[] attrs =
                        new IClasspathAttribute[] { JavaCore.newClasspathAttribute( IClasspathAttribute.OPTIONAL,
                                                                                    "true" ) };

                    cp.put( gensrctPath, JavaCore.newSourceEntry(
                                                                  gensrctPath, //
                                                                  new IPath[0] /* inclusion */,
                                                                  new IPath[0] /* exclusion */,
                                                                  facade.getOutputLocation(), attrs ) );
                }
            }
        }

        setRawClasspath( javaProject, cp, monitor );
    }

    private IPath getFullPath( IMavenProjectFacade facade, File file )
    {
        IProject project = facade.getProject();
        IPath path = MavenProjectUtils.getProjectRelativePath( project, file.getAbsolutePath() );
        return project.getFullPath().append( path );
    }

    private boolean isModelloExecution( MojoExecution mojoExecution )
    {
        VersionRange range;
        try
        {
            range = VersionRange.createFromVersionSpec( MODELLO_VERSION_RANGE );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new IllegalStateException( "Can't parse version range", e );
        }
        DefaultArtifactVersion version = new DefaultArtifactVersion( mojoExecution.getVersion() );

        // XXX supported goal

        boolean supported = MODELLO_GROUP_ID.equals( mojoExecution.getGroupId() ) //
            && MODELLO_ARTIFACT_ID.equals( mojoExecution.getArtifactId() ) //
            && range.containsVersion( version );

        if ( supported )
        {
            return true;
        }

        try
        {
            range = VersionRange.createFromVersionSpec( "[0.0.2-SNAPSHOT,)" );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new IllegalStateException( "Can't parse version range", e );
        }

        return "org.sonatype.plugins".equals( mojoExecution.getGroupId() ) //
            && "modello-plugin-upgrade".equals( mojoExecution.getArtifactId() ) //
            && range.containsVersion( version );
    }

    @Override
    public AbstractBuildParticipant getBuildParticipant( MojoExecution execution )
    {
        if ( isModelloExecution( execution ) )
        {
            return new MojoExecutionBuildParticipant( execution );
        }

        return null;
    }
}

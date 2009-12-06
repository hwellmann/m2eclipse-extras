package org.maven.ide.eclipse.pomproperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;

public class PomPropertiesTest
    extends AsbtractMavenProjectTestCase
{

    public void test001_pomProperties()
        throws Exception
    {
        deleteProject( "resourcefiltering-p001" );
        IProject project = importProject( "projects/pomproperties/pomproperties-p001/pom.xml", new ResolverConfiguration() );
        waitForJobsToComplete();

        IMavenProjectFacade facade = plugin.getMavenProjectManager().create( project, monitor );
        ArtifactKey key = facade.getArtifactKey();

        IPath pomPath =
            project.getFolder(
                               "target/classes/META-INF/maven/" + key.getGroupId() + "/" + key.getArtifactId()
                                   + "/pom.xml" ).getFullPath();

        IPath pomPropertiesPath =
            project.getFolder(
                               "target/classes/META-INF/maven/" + key.getGroupId() + "/" + key.getArtifactId()
                                   + "/pom.properties" ).getFullPath();

        workspace.getRoot().getFile( pomPath ).delete( true, monitor );
        workspace.getRoot().getFile( pomPropertiesPath ).delete( true, monitor );
        project.build( IncrementalProjectBuilder.FULL_BUILD, monitor );

        // pom.xml
        assertTrue( workspace.getRoot().getFile( pomPath ).isAccessible() );

        // standard maven properties
        Properties properties = loadProperties( pomPropertiesPath );
        assertEquals( key.getGroupId(), properties.getProperty( "groupId" ) );
        assertEquals( key.getArtifactId(), properties.getProperty( "artifactId" ) );
        assertEquals( key.getVersion(), properties.getProperty( "version" ) );

        // m2e specific properties
        assertEquals( project.getName(), properties.getProperty( "m2e.projectName" ) );
        assertEquals( project.getLocation().toOSString(), properties.getProperty( "m2e.projectLocation" ) );

    }

    private Properties loadProperties( IPath aPath )
        throws CoreException, IOException
    {
        Properties properties = new Properties();
        InputStream contents = workspace.getRoot().getFile( aPath ).getContents();
        try
        {
            properties.load( contents );
        }
        finally
        {
            contents.close();
        }
        return properties;
    }

}

package org.maven.ide.eclipse.pomproperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class PomPropertiesTest
    extends AbstractMavenProjectTestCase
{
    public void test001_pomProperties()
        throws Exception
    {
        IProject project =
            importProject( "projects/pomproperties/pomproperties-p001/pom.xml", new ResolverConfiguration() );
        waitForJobsToComplete();

        IMavenProjectFacade facade = plugin.getMavenProjectManager().create( project, monitor );
        ArtifactKey key = facade.getArtifactKey();

        IPath pomPath =
            project.getFolder( "target/classes/META-INF/maven/" + key.getGroupId() + "/" + key.getArtifactId()
                                   + "/pom.xml" ).getFullPath();

        IPath pomPropertiesPath =
            project.getFolder( "target/classes/META-INF/maven/" + key.getGroupId() + "/" + key.getArtifactId()
                                   + "/pom.properties" ).getFullPath();

        workspace.getRoot().getFile( pomPath ).delete( true, monitor );
        workspace.getRoot().getFile( pomPropertiesPath ).delete( true, monitor );
        project.build( IncrementalProjectBuilder.FULL_BUILD, monitor );

        // pom.xml
        assertTrue( pomPath + " is not accessible", workspace.getRoot().getFile( pomPath ).isAccessible() );

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

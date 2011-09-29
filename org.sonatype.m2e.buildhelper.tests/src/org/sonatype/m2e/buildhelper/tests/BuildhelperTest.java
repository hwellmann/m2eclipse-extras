package org.sonatype.m2e.buildhelper.tests;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

@SuppressWarnings( "restriction" )
public class BuildhelperTest
    extends AbstractMavenProjectTestCase
{
    public void test_p001_simple()
        throws Exception
    {
        IProject project = importBuildHelperProject( "buildhelper-001" );
        IJavaProject javaProject = JavaCore.create( project );
        IClasspathEntry[] classpath = javaProject.getRawClasspath();

        assertEquals( 4, classpath.length );

        assertEquals( new Path( "/buildhelper-001/src/main/java" ), classpath[0].getPath() );
        assertEquals( new Path( "/buildhelper-001/src/custom/java" ), classpath[1].getPath() );
        assertEquals( "org.eclipse.jdt.launching.JRE_CONTAINER", classpath[2].getPath().segment( 0 ) );
        assertEquals( new Path( "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER" ), classpath[3].getPath() );
    }

    public void test_p002_resources()
        throws Exception
    {
        IProject project = importBuildHelperProject( "buildhelper-002" );
        IJavaProject javaProject = JavaCore.create( project );
        IClasspathEntry[] classpath = javaProject.getRawClasspath();

        assertEquals( 8, classpath.length );
        assertEquals( new Path( "/buildhelper-002/src/main/java" ), classpath[0].getPath() );
        assertEquals( new Path( "/buildhelper-002/src/custom/main/java" ), classpath[1].getPath() );
        assertEquals( new Path( "/buildhelper-002/src/main/resources" ), classpath[2].getPath() );
        assertEquals( new Path( "/buildhelper-002/src/custom/main/resources" ), classpath[3].getPath() );
        assertEquals( new Path( "/buildhelper-002/src/custom/test/java" ), classpath[4].getPath() );
        assertEquals( new Path( "/buildhelper-002/src/custom/test/resources" ), classpath[5].getPath() );
        assertEquals( "org.eclipse.jdt.launching.JRE_CONTAINER", classpath[6].getPath().segment( 0 ) );
        assertEquals( new Path( "org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER" ), classpath[7].getPath() );

        File target = project.findMember( "target" ).getRawLocation().toFile();
        assertTrue( target + " does not exist", target.exists() );
        assertTrue( "Class", new File( target, "classes/buildhelper002/custom/CustomTreeClass.class" ).exists() );
        assertTrue( "Resource", new File( target, "classes/buildhelper002/custom/customTree.txt" ).exists() );
        assertTrue( "Test Class",
                    new File( target, "test-classes/buildhelper002/custom/CustomTreeClassTest.class" ).exists() );
        assertTrue( "Test Resource",
                    new File( target, "test-classes/buildhelper002/custom/customTreeTest.txt" ).exists() );
    }

    private IProject importBuildHelperProject( String name )
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project = importProject( "projects/buildhelper/" + name + "/pom.xml", configuration );
        waitForJobsToComplete();

        project.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        waitForJobsToComplete();

        assertNoErrors( project );
        return project;
    }
}

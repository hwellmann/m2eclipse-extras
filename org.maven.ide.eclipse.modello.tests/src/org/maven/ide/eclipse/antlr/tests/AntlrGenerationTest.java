package org.maven.ide.eclipse.antlr.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.common.AbstractMavenProjectTestCase;

public class AntlrGenerationTest
    extends AbstractMavenProjectTestCase
{
    public void test_p001_simple()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project1 = importProject( "projects/antlr/antlr-p001/pom.xml", configuration );
        waitForJobsToComplete();

        project1.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        project1.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );
        waitForJobsToComplete();

        assertNoErrors( project1 );

        IJavaProject javaProject1 = JavaCore.create( project1 );
        IClasspathEntry[] cp1 = javaProject1.getRawClasspath();

        assertEquals( new Path( "/antlr-p001/target/generated-sources/antlr" ), cp1[3].getPath() );

        assertTrue( project1.getFile( "target/generated-sources/antlr/test/SampleParser.java" ).isSynchronized( IResource.DEPTH_ZERO ) );
        assertTrue( project1.getFile( "target/generated-sources/antlr/test/SampleParser.java" ).isAccessible() );
    }

    public void test_p001_simple_v3()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project1 = importProject( "projects/antlr/antlr-v3-p001/pom.xml", configuration );
        waitForJobsToComplete();

        project1.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        project1.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );
        waitForJobsToComplete();

        assertNoErrors( project1 );

        IJavaProject javaProject1 = JavaCore.create( project1 );
        IClasspathEntry[] cp1 = javaProject1.getRawClasspath();

        assertEquals( new Path( "/antlr-v3-p001/target/generated-sources/antlr" ), cp1[3].getPath() );

        assertTrue( project1.getFile( "target/generated-sources/antlr/test/SampleParser.java" ).isSynchronized( IResource.DEPTH_ZERO ) );
        assertTrue( project1.getFile( "target/generated-sources/antlr/test/SampleParser.java" ).isAccessible() );
    }

}

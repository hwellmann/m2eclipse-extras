package org.sonatype.m2e.buildhelper.tests;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class BuildhelperTest
    extends AbstractMavenProjectTestCase
{
    public void test_p001_simple()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project1 = importProject( "projects/buildhelper/buildhelper-001/pom.xml", configuration );
        waitForJobsToComplete();

        project1.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        waitForJobsToComplete();

        assertNoErrors( project1 );

        IJavaProject javaProject1 = JavaCore.create( project1 );
        IClasspathEntry[] cp1 = javaProject1.getRawClasspath();

        assertEquals( new Path( "/buildhelper-001/src/custom/java" ), cp1[3].getPath() );
    }

}

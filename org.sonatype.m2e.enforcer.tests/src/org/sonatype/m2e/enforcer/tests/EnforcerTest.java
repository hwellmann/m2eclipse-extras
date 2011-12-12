package org.sonatype.m2e.enforcer.tests;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class EnforcerTest
    extends AbstractMavenProjectTestCase
{
    public void test_p001_simple()
        throws Exception
    {
        ResolverConfiguration configuration = new ResolverConfiguration();
        IProject project1 = importProject( "projects/enforcer/enforcer-p001/pom.xml", configuration );
        waitForJobsToComplete();

        project1.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
        project1.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );
        waitForJobsToComplete();

        assertNoErrors( project1 );

    }
    public void test_p002_failure()
    throws Exception 
    {
    	ResolverConfiguration configuration = new ResolverConfiguration();
    	IProject project1 = importProject( "projects/enforcer/enforcer-p002/pom.xml", configuration );
    	waitForJobsToComplete();

    	project1.build( IncrementalProjectBuilder.FULL_BUILD, monitor );
    	project1.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );
    	waitForJobsToComplete();

    	List<IMarker> markers = findErrorMarkers(project1);
    	boolean foundErrorMarker = Boolean.FALSE;
    	for(IMarker marker: markers) {
    		int severity = (Integer) marker.getAttribute(IMarker.SEVERITY);
    		String message = (String) marker.getAttribute(IMarker.MESSAGE);
    		if(severity == IMarker.SEVERITY_ERROR && message.contains("enforce-ban-duplicate-classes")) {
    			foundErrorMarker = Boolean.TRUE;
    		}
    	}

    	assertTrue(foundErrorMarker);

    }

}

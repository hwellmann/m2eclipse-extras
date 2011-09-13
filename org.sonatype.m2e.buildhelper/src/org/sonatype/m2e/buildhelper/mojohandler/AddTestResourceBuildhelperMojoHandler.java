package org.sonatype.m2e.buildhelper.mojohandler;

import org.apache.maven.model.Resource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.sonatype.m2e.buildhelper.BuildContext;
import org.sonatype.m2e.buildhelper.ConfigureRawClasspathContext;
import org.sonatype.m2e.buildhelper.util.ClasspathUtils;

/**
 * {@link BuildhelperMojoHandler} that supports the <tt>add-test-resource</tt> goal.
 * 
 * @author Phillip Webb
 */
public class AddTestResourceBuildhelperMojoHandler
    extends AbstractBuildhelperMojoHandler
{
    @Override
    protected String getGoal()
    {
        return "add-test-resource";
    }

    @Override
    public void configureRawClasspath( ConfigureRawClasspathContext context )
        throws CoreException
    {
        Resource[] resources = context.getParameterValue( "resources", Resource[].class );
        IPath outputLocation = context.getTestOutputLocation();
        String charset = context.getTestResourceEncoding();
        ClasspathUtils.addResources( context, resources, outputLocation, charset );
    }

    @Override
    public void build( BuildContext context )
    {
        context.executeMojo();
    }
}

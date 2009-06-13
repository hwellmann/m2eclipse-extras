package org.maven.ide.eclipse.modello.internal;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.MojoExecutionBuildParticipant;

public class ModelloBuildParticipant
    extends MojoExecutionBuildParticipant
{

    public ModelloBuildParticipant( MojoExecution execution )
    {
        super( execution );
    }

    @Override
    public void clean( IProgressMonitor monitor )
        throws CoreException
    {
//        IMavenProjectFacade facade = getMavenProjectFacade();
//
//        File gensrcFolder =
//            getParameterValue( getSession(), getMojoExecution(), "outputDirectory", File.class );
//        IPath gensrctPath = facade.getFullPath( gensrcFolder );
//
//        // TODO Auto-generated method stub
//        super.clean( monitor );
    }
}

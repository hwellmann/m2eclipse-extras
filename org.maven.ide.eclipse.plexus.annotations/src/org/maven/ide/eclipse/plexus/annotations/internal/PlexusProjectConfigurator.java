package org.maven.ide.eclipse.plexus.annotations.internal;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

public class PlexusProjectConfigurator extends AbstractProjectConfigurator {

  private static final String MAVEN_PLUGIN_ID = "org.codehaus.plexus:plexus-component-metadata";

  public PlexusProjectConfigurator() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    // TODO Auto-generated method stub

  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade facade, MojoBinding mojoBinding) {
    if (equalPluginId(MAVEN_PLUGIN_ID, mojoBinding)) {
      if ("generate-metadata".equals(mojoBinding.getGoal())) {
        return new PlexusBuildParticipant(false);
      } else if ("generate-test-metadata".equals(mojoBinding.getGoal())) {
        return new PlexusBuildParticipant(true);
      }
    }
    return null;
  }
}

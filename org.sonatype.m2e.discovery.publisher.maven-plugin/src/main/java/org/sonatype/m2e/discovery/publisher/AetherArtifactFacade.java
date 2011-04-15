package org.sonatype.m2e.discovery.publisher;
import java.io.File;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.tycho.p2.IArtifactFacade;

public class AetherArtifactFacade
    implements IArtifactFacade
{
    private final Artifact artifact;

    public AetherArtifactFacade( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public String getArtifactId()
    {
        return artifact.getArtifactId();
    }

    public String getClassidier()
    {
        return artifact.getClassifier();
    }

    public String getGroupId()
    {
        return artifact.getGroupId();
    }

    public File getLocation()
    {
        return artifact.getFile();
    }

    public String getPackagingType()
    {
        return null;
    }

    public String getVersion()
    {
        return artifact.getVersion();
    }
}

package org.sonatype.m2e.discovery.publisher.p2.facade;

import java.io.OutputStream;

public interface P2Facade
{
    /**
     * @return true if the specified installable unit is a bundle
     */
    public boolean getIU( String p2RepoUri, String iuId, String iuVersion, OutputStream destination );
}

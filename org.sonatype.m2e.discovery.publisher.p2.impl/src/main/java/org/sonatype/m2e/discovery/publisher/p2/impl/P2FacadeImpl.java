package org.sonatype.m2e.discovery.publisher.p2.impl;

import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.sonatype.m2e.discovery.publisher.p2.facade.P2Facade;

@SuppressWarnings( "restriction" )
public class P2FacadeImpl
    implements P2Facade
{
    private IProvisioningAgent agent;

    private IProgressMonitor monitor = new NullProgressMonitor();

    private IProvisioningAgent getP2Agent()
        throws ProvisionException
    {
        if ( agent != null )
        {
            return agent;
        }

        agent = Activator.newProvisioningAgent();
        return agent;
    }

    private IArtifactDescriptor getArtifactDescriptor( IArtifactRepository artifactRepository,
                                                       IMetadataRepository metadataRepository, String iuId,
                                                       String iuVersion )
    {
        IInstallableUnit iu = queryIU( metadataRepository, iuId, iuVersion );
        if ( "true".equals( iu.getProperty( "org.eclipse.equinox.p2.type.group" ) ) )
        {
            // TODO This is a "group" feature IU - inspect it
            System.out.println( iuId + " is a group feature IU" );
            return null;
        }

        Collection<IProvidedCapability> capabilities = iu.getProvidedCapabilities();
        for ( IProvidedCapability capability : capabilities )
        {
            if ( "org.eclipse.equinox.p2.eclipse.type".equals( capability.getNamespace() ) )
            {
                if ( "feature".equals( capability.getName() ) )
                {
                    // TODO This is a feature IU - inspect it
                    System.out.println( iuId + " is a feature IU" );
                    return null;
                }
                if ( "bundle".equals( capability.getName() ) )
                {
                    break;
                }
                throw new RuntimeException( "Unsupported IU: org.eclipse.equinox.p2.eclipse.type="
                    + capability.getName() );
            }
        }
        Collection<IArtifactKey> artifactKeys = iu.getArtifacts();
        if ( artifactKeys.isEmpty() )
        {
            throw new RuntimeException( "Cannot find artifacts for installable unit with id=" + iuId + ", version="
                + iuVersion );
        }
        if ( artifactKeys.size() > 1 )
        {
            throw new RuntimeException( "Too many artifacts for installable unit with id=" + iuId + ", version="
                + iuVersion );
        }

        IArtifactKey artifactKey = artifactKeys.iterator().next();
        IArtifactDescriptor[] artifactDescriptors = artifactRepository.getArtifactDescriptors( artifactKey );
        if ( artifactDescriptors == null || artifactDescriptors.length == 0 )
        {
            throw new RuntimeException( "Cannot find artifact for installable unit with id=" + iuId + ", version="
                + iuVersion );
        }
        return artifactDescriptors[0];
    }

    private IInstallableUnit queryIU( IMetadataRepository metadataRepository, String iuId, String iuVersion )
    {
        VersionRange versionRange = new VersionRange( iuVersion );
        IQuery<IInstallableUnit> query = QueryUtil.createIUQuery( iuId, versionRange );
        IQueryResult<IInstallableUnit> matches = metadataRepository.query( query, monitor );
        if ( matches.isEmpty() )
        {
            throw new RuntimeException( "Cannot find installable unit with id=" + iuId + ", version=" + iuVersion );
        }
        // TODO get the latest version
        return matches.iterator().next();
    }

    public boolean getIU( String p2RepoUri, String iuId, String iuVersion, OutputStream destination )
    {
        try
        {
            IArtifactRepositoryManager artifactRepositoryManager =
                (IArtifactRepositoryManager) getP2Agent().getService( IArtifactRepositoryManager.SERVICE_NAME );
            if ( artifactRepositoryManager == null )
            {
                throw new IllegalStateException( "No artifact repository manager found" ); //$NON-NLS-1$
            }
            IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();

            URI repoUri = new URI( p2RepoUri );
            IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository( repoUri, monitor );
            IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository( repoUri, monitor );
            if ( iuVersion == null )
            {
                iuVersion = "0.0.0";
            }
            IArtifactDescriptor artifactDescriptor =
                getArtifactDescriptor( artifactRepository, metadataRepository, iuId, iuVersion );
            if ( artifactDescriptor != null )
            {
                artifactRepository.getRawArtifact( artifactDescriptor, destination, monitor );
                return true;
            }
            return false;
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
        catch ( ProvisionException e )
        {
            throw new RuntimeException( e );
        }
    }

    private IMetadataRepositoryManager getMetadataRepositoryManager()
        throws ProvisionException
    {
        IMetadataRepositoryManager metadataRepositoryManager =
            (IMetadataRepositoryManager) getP2Agent().getService( IMetadataRepositoryManager.SERVICE_NAME );
        if ( metadataRepositoryManager == null )
        {
            throw new IllegalStateException( "No metadata repository manager found" ); //$NON-NLS-1$
        }
        return metadataRepositoryManager;
    }

    public boolean validateLicense( String p2RepoUri, String iuId, String iuVersion )
    {
        try
        {
            IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
            URI repoUri = new URI( p2RepoUri );
            IMetadataRepository metadataRepository = metadataRepositoryManager.loadRepository( repoUri, monitor );
            if ( iuVersion == null )
            {
                iuVersion = "0.0.0";
            }
            IInstallableUnit rootIU = queryIU( metadataRepository, iuId, iuVersion );

            // select specified IU and its "hard" dependencies
            Map<String, String> context = Collections.<String, String> emptyMap();
            PermissiveSlicer slicer =
                new PermissiveSlicer( metadataRepository, context, false /* includeOptionalDependencies */,
                                      false /* everythingGreedy */, false /* evalFilterTo */,
                                      true /* considerOnlyStrictDependency */, false /* onlyFilteredRequirements */);
            IQueryable<IInstallableUnit> slice = slicer.slice( new IInstallableUnit[] { rootIU }, monitor );
            if ( slice == null )
            {
                throw new RuntimeException( "Can't find specified IU or one of its dependencies" );
            }
            Iterator<IInstallableUnit> iuIter = slice.query( QueryUtil.ALL_UNITS, monitor ).iterator();
            while ( iuIter.hasNext() )
            {
                IInstallableUnit iu = iuIter.next();
                Collection<ILicense> licenses = iu.getLicenses( null );
                if ( licenses != null && !licenses.isEmpty() )
                {
                    return true;
                }
            }
            return false;
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
        catch ( ProvisionException e )
        {
            throw new RuntimeException( e );
        }

    }
}

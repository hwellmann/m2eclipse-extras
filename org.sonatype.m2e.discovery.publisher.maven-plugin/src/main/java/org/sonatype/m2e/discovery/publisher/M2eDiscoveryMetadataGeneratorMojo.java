package org.sonatype.m2e.discovery.publisher;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Reader;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Writer;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.sonatype.m2e.discovery.catalog.model.DiscoveryCatalog;
import org.sonatype.m2e.discovery.catalog.model.DiscoveryCatalogItem;
import org.sonatype.m2e.discovery.catalog.model.DiscoveryCategory;
import org.sonatype.m2e.discovery.catalog.model.DiscoveryIcon;
import org.sonatype.m2e.discovery.catalog.model.IUData;
import org.sonatype.m2e.discovery.catalog.model.InputLocation;
import org.sonatype.m2e.discovery.catalog.model.InputLocationTracker;
import org.sonatype.m2e.discovery.catalog.model.MavenData;
import org.sonatype.m2e.discovery.catalog.model.P2Data;
import org.sonatype.m2e.discovery.catalog.model.io.xpp3.DiscoveryCatalogModelXpp3ReaderEx;
import org.sonatype.m2e.discovery.publisher.p2.facade.P2Facade;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;

/**
 * @goal generate
 * @phase generate-resources
 */
public class M2eDiscoveryMetadataGeneratorMojo
    extends AbstractMojo
{
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

    private static final List<String> SUPPORTED_M2E_VERSION = Arrays.asList( "1.0", "1.1" );

    /**
     * Base directory of the project.
     * 
     * @parameter default-value="${project.build.directory}/catalog"
     * @readonly
     * @required
     */
    private File outputdir;

    /**
     * Catalog descriptor file
     * 
     * @parameter default-value="${project.basedir}/connectors.xml"
     * @readonly
     * @required
     */
    private File descriptor;

    /**
     * @parameter default-value="1.0"
     * @required
     */
    private String m2eversion;

    private P2Facade p2Facade;

    /** @component */
    private EquinoxServiceFactory equinox;

    public void execute()
        throws MojoExecutionException
    {
        if ( !SUPPORTED_M2E_VERSION.contains( m2eversion ) )
        {
            throw new MojoExecutionException( "Specified m2e version " + m2eversion + " is not supported" );
        }

        getLog().info( "Generating discovery catalog for m2e version " + m2eversion );

        try
        {
            p2Facade = equinox.getService( P2Facade.class );

            outputdir.mkdirs();

            deleteDirectoryContent( new File( outputdir, MavenDiscovery.LIFECYCLE_PATH ) );
            File tempFile = File.createTempFile( "M2eDiscoveryMetadataGeneratorMojo", ".jar" );
            tempFile.deleteOnExit();

            List<P2Data> missingLicense = new ArrayList<P2Data>();

            Set<String> allids = new HashSet<String>();

            DiscoveryCatalog catalog = loadDiscoveryCatalog();
            for ( DiscoveryCatalogItem catalogItem : catalog.getCatalogItems() )
            {
                if ( !isCompatibleWithM2EVersion( catalogItem ) )
                {
                    getLog().debug( "Catalog item id " + catalogItem.getId() + " m2e compatibility "
                                        + catalogItem.getM2EVersions() + " does not include version " + m2eversion );
                    continue;
                }

                if ( !allids.add( catalogItem.getId() ) )
                {
                    throw new RuntimeException( "Duplicate catalog item id " + catalogItem.getId() );
                }

                getLog().debug( "Processing catalog item with id " + catalogItem.getId() );
                validateCatalogItem( catalog, catalogItem );

                boolean hasLifecycleMappings = false;
                LifecycleMappingMetadataSource mergedLifecycleMappingMetadataSource =
                    new LifecycleMappingMetadataSource();
                Xpp3Dom mergedPluginXmlDom = new _Xpp3Dom( "plugin" );

                P2Data p2Data = catalogItem.getP2Data();
                MavenData mavenData = catalogItem.getMavenData();
                if ( p2Data != null )
                {
                    String p2RepoUrl = p2Data.getRepositoryUrl();
                    if ( p2Data.getLifecycleMappingIUs().size() > 0 )
                    {
                        for ( IUData iuData : p2Data.getLifecycleMappingIUs() )
                        {
                            getLog().debug( "Loading bundle p2RepoUrl=" + p2RepoUrl + ", iuId=" + iuData.getIuId()
                                                + ", iuVersion=" + iuData.getIuVersion() );
                            boolean isBundle =
                                downloadIU( p2RepoUrl, iuData.getIuId(), iuData.getIuVersion(), tempFile );
                            if ( !isBundle )
                            {
                                throw new RuntimeException( "Not a bundle: iuId=" + iuData.getIuId() + ", iuVersion="
                                    + iuData.getIuVersion() );
                            }
                            hasLifecycleMappings =
                                processBundle( tempFile, mergedLifecycleMappingMetadataSource, mergedPluginXmlDom );
                        }
                    }
                    else
                    {
                        String iuId = p2Data.getIuId();
                        String iuVersion = p2Data.getIuVersion();
                        getLog().debug( "Loading IU p2RepoUrl=" + p2RepoUrl + ", iuId=" + iuId + ", iuVersion="
                                            + iuVersion );
                        boolean isBundle = downloadIU( p2RepoUrl, iuId, iuVersion, tempFile );
                        if ( isBundle )
                        {
                            hasLifecycleMappings =
                                processBundle( tempFile, mergedLifecycleMappingMetadataSource, mergedPluginXmlDom );
                        }
                    }
                    if ( !p2Facade.validateLicense( p2RepoUrl, p2Data.getIuId(), p2Data.getIuVersion() ) )
                    {
                        missingLicense.add( p2Data );
                    }
                }
                else if ( mavenData != null )
                {
                    throw new RuntimeException(
                                                "Publishing from maven repositories is disabled due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=346830" );
                }
                else
                {
                    throw new RuntimeException( "No remote data specified for catalog item id=" + catalogItem.getId() );
                }

                if ( hasLifecycleMappings )
                {
                    File destinationDirectory = new File( outputdir, MavenDiscovery.LIFECYCLE_PATH );
                    writeLifecycleMappingMetadata( mergedLifecycleMappingMetadataSource,
                                                   new File( destinationDirectory, catalogItem.getId()
                                                       + MavenDiscovery.LIFECYCLE_EXT ) );
                    writeXml( mergedPluginXmlDom, new File( destinationDirectory, catalogItem.getId()
                        + MavenDiscovery.PLUGINXML_EXT ) );
                }
            }

            if ( !missingLicense.isEmpty() )
            {
                StringBuilder sb = new StringBuilder( "The following items do not have required license information:" );
                for ( P2Data p2Data : missingLicense )
                {
                    sb.append( '\n' ).append( p2Data.getIuId() ).append( '@' ).append( p2Data.getIuVersion() );
                    sb.append( " from " ).append( p2Data.getRepositoryUrl() );
                }
                throw new MojoExecutionException( sb.toString() );
            }

            generateMainPluginXml( catalog );
        }
        catch ( RuntimeException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private boolean isCompatibleWithM2EVersion( DiscoveryCatalogItem catalogItem )
        throws MojoExecutionException
    {
        StringTokenizer st = new StringTokenizer( catalogItem.getM2EVersions(), "," );

        boolean compatible = false;

        while ( st.hasMoreTokens() )
        {
            String version = st.nextToken().trim();

            if ( !SUPPORTED_M2E_VERSION.contains( version ) )
            {
                throw new MojoExecutionException( "Catalog item with id " + catalogItem.getId()
                    + " specifies unknown/unsuported m2e version " + version );
            }

            compatible |= version.equals( m2eversion );
        }

        return compatible;
    }

    private void validateCatalogItem( InputLocationTracker locationTracker, DiscoveryCatalogItem item )
    {
        List<String> messages = new ArrayList<String>();
        if ( nv( item.getId() ) == null )
        {
            messages.add( "id must be specified" );
        }
        if ( nv( item.getProvider() ) == null )
        {
            messages.add( "provider must be specified" );
        }

        if ( !messages.isEmpty() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Problems with <catalogItem>" );

            InputLocation location = item.getLocation( "" );
            if ( location != null )
            {
                sb.append( '[' ).append( location.toString() ).append( ']' );
            }

            for ( String msg : messages )
            {
                sb.append( "\n   " ).append( msg );
            }
            throw new RuntimeException( sb.toString() );
        }
    }

    private static String nv( String str )
    {
        if ( str != null && !"".equals( str.trim() ) )
        {
            return str;
        }
        return null;
    }

    private boolean processBundle( File bundleJar, LifecycleMappingMetadataSource mergedLifecycleMappingMetadataSource,
                                   Xpp3Dom mergedPluginXmlDom )
        throws IOException, XmlPullParserException
    {
        boolean hasLifecycleMappings = false;
        FileInputStream fis = null;
        JarInputStream jis = null;
        try
        {
            fis = new FileInputStream( bundleJar );
            jis = new JarInputStream( fis );
            JarEntry jarEntry = jis.getNextJarEntry();
            while ( jarEntry != null )
            {
                if ( LifecycleMappingFactory.LIFECYCLE_MAPPING_METADATA_SOURCE_NAME.equals( jarEntry.getName() ) )
                {
                    byte[] jarEntryContent = getByteContent( jis );
                    mergeLifecycleMappingMetadata( jarEntryContent, mergedLifecycleMappingMetadataSource );
                    hasLifecycleMappings = true;
                }
                else if ( "plugin.xml".equals( jarEntry.getName() ) )
                {
                    byte[] jarEntryContent = getByteContent( jis );
                    mergePluginXml( jarEntryContent, mergedPluginXmlDom );
                }
                jarEntry = jis.getNextJarEntry();
            }
            return hasLifecycleMappings;
        }
        finally
        {
            IOUtil.close( fis );
            IOUtil.close( jis );
        }
    }

    private boolean downloadIU( String p2RepoUri, String iuId, String iuVersion, File destination )
        throws IOException
    {
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( destination );
            boolean isBundle = p2Facade.getIU( p2RepoUri, iuId, iuVersion, fos );
            fos.flush();
            return isBundle;
        }
        finally
        {
            IOUtil.close( fos );
        }
    }

    private void deleteDirectoryContent( File directory )
        throws IOException
    {
        if ( !directory.exists() )
        {
            return;
        }
        for ( File file : directory.listFiles() )
        {
            if ( !file.isDirectory() )
            {
                String filename = file.getName();
                if ( filename.endsWith( MavenDiscovery.PLUGINXML_EXT )
                    || filename.endsWith( MavenDiscovery.LIFECYCLE_EXT ) )
                {
                    if ( !file.delete() )
                    {
                        throw new IOException( "Cannot delete file " + file.getAbsolutePath() );
                    }
                }
            }
        }
    }

    private void writeXml( Xpp3Dom source, File destination )
        throws IOException
    {
        destination.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream( destination );
        try
        {
            writeXml( source, fos );
        }
        finally
        {
            IOUtil.close( fos );
        }
    }

    private void writeXml( Xpp3Dom source, OutputStream os )
        throws IOException
    {
        MXSerializer mx = new MXSerializer();
        mx.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  " );
        mx.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n" );
        String encoding = "UTF-8";
        mx.setOutput( os, encoding );
        mx.startDocument( encoding, null );
        mx.comment( getHeader() );
        mx.ignorableWhitespace( "\n" );
        source.writeToSerializer( null, mx );
        mx.flush();
        os.flush();
    }

    private String getHeader()
    {
        String version = getClass().getPackage().getImplementationVersion();
        return "\n" + //
            "=================== DO NOT EDIT THIS FILE ====================\n" + //
            "Generated by M2E Discovery Publisher" + ( ( version == null ) ? "" : ( ' ' + version ) ) + " on " + //
            DATE_FORMAT.format( new Date() ) + ",\n" + //
            "any modifications will be overwritten.\n" + //
            "==============================================================\n";
    }

    private void writeLifecycleMappingMetadata( LifecycleMappingMetadataSource source, File destination )
        throws IOException
    {
        destination.getParentFile().mkdirs();
        LifecycleMappingMetadataSourceXpp3Writer writer = new LifecycleMappingMetadataSourceXpp3Writer();
        FileOutputStream fos = new FileOutputStream( destination );
        try
        {
            writer.write( fos, source );
            fos.flush();
        }
        finally
        {
            IOUtil.close( fos );
        }
    }

    private void mergePluginXml( byte[] sourceBytes, Xpp3Dom destination )
        throws IOException, XmlPullParserException
    {
        ByteArrayInputStream is = new ByteArrayInputStream( sourceBytes );
        try
        {
            Xpp3Dom sourceDom = Xpp3DomBuilder.build( new XmlStreamReader( is ) );
            for ( Xpp3Dom extensionDom : sourceDom.getChildren( "extension" ) )
            {
                String extensionPoint = extensionDom.getAttribute( "point" );
                if ( LifecycleMappingFactory.EXTENSION_PROJECT_CONFIGURATORS.equals( extensionPoint )
                    || LifecycleMappingFactory.EXTENSION_LIFECYCLE_MAPPINGS.equals( extensionPoint ) )
                {
                    destination.addChild( extensionDom );
                }
            }
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    private void mergeLifecycleMappingMetadata( byte[] sourceBytes, LifecycleMappingMetadataSource destination )
        throws IOException, XmlPullParserException
    {
        LifecycleMappingMetadataSourceXpp3Reader reader = new LifecycleMappingMetadataSourceXpp3Reader();
        ByteArrayInputStream is = new ByteArrayInputStream( sourceBytes );
        try
        {
            LifecycleMappingMetadataSource source = reader.read( is );
            for ( LifecycleMappingMetadata lmm : source.getLifecycleMappings() )
            {
                destination.addLifecycleMapping( lmm );
            }
            for ( PluginExecutionMetadata pem : source.getPluginExecutions() )
            {
                destination.addPluginExecution( pem );
            }
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    private byte[] getByteContent( InputStream is )
        throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try
        {
            byte[] buffer = new byte[0x1000];
            for ( int n = 0; ( n = is.read( buffer ) ) > -1; os.write( buffer, 0, n ) )
                ;
            return os.toByteArray();
        }
        finally
        {
            IOUtil.close( os );
        }
    }

    private DiscoveryCatalog loadDiscoveryCatalog()
        throws IOException, XmlPullParserException
    {
        InputStream is = new FileInputStream( descriptor );
        try
        {
            return new DiscoveryCatalogModelXpp3ReaderEx().read( is, true );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    private void generateMainPluginXml( DiscoveryCatalog catalog )
        throws IOException, MojoExecutionException
    {
        Xpp3Dom pluginXmlDom = new _Xpp3Dom( "plugin" );

        if ( catalog.getCategories().size() > 0 )
        {
            Xpp3Dom extensionDom = new _Xpp3Dom( "extension" );
            extensionDom.setAttribute( "point", "org.eclipse.mylyn.discovery.core.connectorDiscovery" );
            pluginXmlDom.addChild( extensionDom );
            for ( DiscoveryCategory category : catalog.getCategories() )
            {
                Xpp3Dom categoryDom = discoveryCategory2PluginXml( category );
                extensionDom.addChild( categoryDom );
            }
        }

        if ( catalog.getCatalogItems().size() > 0 )
        {
            Xpp3Dom extensionDom = new _Xpp3Dom( "extension" );
            extensionDom.setAttribute( "point", "org.eclipse.mylyn.discovery.core.connectorDiscovery" );
            pluginXmlDom.addChild( extensionDom );
            for ( DiscoveryCatalogItem item : catalog.getCatalogItems() )
            {
                if ( isCompatibleWithM2EVersion( item ) )
                {
                    Xpp3Dom itemDom = discoveryItem2PluginXml( item );
                    extensionDom.addChild( itemDom );
                }
            }
        }

        writeXml( pluginXmlDom, new File( outputdir, "plugin.xml" ) );
    }

    private Xpp3Dom discoveryCategory2PluginXml( DiscoveryCategory category )
    {
        Xpp3Dom result = new _Xpp3Dom( "connectorCategory" );
        result.setAttribute( "id", category.getId() );
        result.setAttribute( "name", category.getName() );
        result.setAttribute( "description", category.getDescription() );
        result.setAttribute( "relevance", category.getRelevance() );

        for ( String groupId : category.getGroupIds() )
        {
            Xpp3Dom groupDom = new _Xpp3Dom( "group" );
            groupDom.setAttribute( "id", groupId );
            result.addChild( groupDom );
        }

        DiscoveryIcon icon = category.getIcon();
        if ( icon != null )
        {
            Xpp3Dom iconDom = new _Xpp3Dom( "icon" );
            iconDom.setAttribute( "image" + icon.getSize(), icon.getPath() );
            result.addChild( iconDom );
        }

        return result;
    }

    private Xpp3Dom discoveryItem2PluginXml( DiscoveryCatalogItem item )
    {
        Xpp3Dom result = new _Xpp3Dom( "connectorDescriptor" );
        result.setAttribute( "id", item.getId() );
        result.setAttribute( "name", item.getName() );
        result.setAttribute( "description", item.getDescription() );
        result.setAttribute( "categoryId", item.getCategoryId() );
        result.setAttribute( "groupId", item.getGroupId() );
        result.setAttribute( "kind", item.getKind() );
        result.setAttribute( "license", item.getLicense() );
        result.setAttribute( "provider", item.getProvider() );
        result.setAttribute( "siteUrl", item.getP2Data().getRepositoryUrl() );

        Xpp3Dom iuDom = new _Xpp3Dom( "iu" );
        iuDom.setAttribute( "id", item.getP2Data().getIuId() );
        iuDom.setAttribute( "version", item.getP2Data().getIuVersion() );
        result.addChild( iuDom );

        for ( IUData rootIU : item.getP2Data().getRootIUs() )
        {
            Xpp3Dom rootIUDom = new _Xpp3Dom( "iu" );
            rootIUDom.setAttribute( "id", rootIU.getIuId() );
            if ( rootIU.getIuVersion() != null )
            {
                rootIUDom.setAttribute( "version", rootIU.getIuVersion() );
            }
            result.addChild( rootIUDom );
        }

        Xpp3Dom overviewDom = new _Xpp3Dom( "overview" );
        overviewDom.setAttribute( "summary", item.getOverview().getSummary() );
        overviewDom.setAttribute( "url", item.getOverview().getUrl() );
        result.addChild( overviewDom );

        return result;
    }

    private static class _Xpp3Dom
        extends Xpp3Dom
    {
        public _Xpp3Dom( String name )
        {
            super( name );
        }

        @Override
        public void setAttribute( String name, String value )
        {
            if ( value != null )
            {
                super.setAttribute( name, value );
            }
        }
    }
}

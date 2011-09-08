/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sonatype.m2e.tests.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * A helper for the tests to start a SVN server. Create an instance of this class, use its mutators to configure the
 * server and finally call {@link #start()}. This requires the external commands {@code svnadmin} and {@code svnserve}
 * to be on the {@code PATH}.
 * 
 * @author Benjamin Bentmann
 */
public class SvnServer
{

    public static final int DEFAULT_PORT = 3690;

    private int port;

    private File dumpFile;

    private File confDir;

    private Process server;

    private File basedir;

    private int svnPort;

    /**
     * Sets the port to start the server on.
     * 
     * @param port The port to start the server on, may be {@code 0} to pick a random unused port, if negative only the
     *            repository will be setup but no server will be started.
     * @return This server, never {@code null}.
     */
    public SvnServer setPort( int port )
    {
        this.port = port;

        return this;
    }

    /**
     * Gets the port number of the server.
     * 
     * @return The port number of the server.
     */
    public int getPort()
    {
        if ( server != null )
        {
            return svnPort;
        }
        else
        {
            return port;
        }
    }

    /**
     * Gets the base URL to the server's connector, e.g. {@code "svn://localhost:3690"}.
     * 
     * @return The base URL without trailing slash to the server's connector, never {@code null}.
     */
    public String getUrl()
    {
        return "svn://localhost:" + getPort();
    }

    /**
     * Sets the dump file to import into the test repository.
     * 
     * @param dumpFile The path to the dump, may be {@code null}.
     * @return This server, never {@code null}.
     */
    public SvnServer setDumpFile( String dumpFile )
    {
        this.dumpFile = ( dumpFile != null ) ? new File( dumpFile ).getAbsoluteFile() : null;
        return this;
    }

    /**
     * Sets the configuration directory to use for the test repository.
     * 
     * @param confDir The path to the conf directory, may be {@code null}.
     * @return This server, never {@code null}.
     */
    public SvnServer setConfDir( String confDir )
    {
        this.confDir = ( confDir != null ) ? new File( confDir ).getAbsoluteFile() : null;

        return this;
    }

    /**
     * Gets the root directory of the test repository.
     * 
     * @return The root directory of the test repository or {@code null} if the server is not running.
     */
    public File getRootDir()
    {
        return basedir;
    }

    /**
     * Creates the test repository in a temporary directory, imports the dump file and eventually starts the server.
     * Trying to start an already running server has no effect.
     * 
     * @return This server, never {@code null}.
     * @throws Exception If the server could not be started.
     */
    public SvnServer start()
        throws Exception
    {
        if ( basedir != null )
        {
            return this;
        }

        basedir = File.createTempFile( "s2svnit", ".dir" );
        basedir.delete();


        createSvnRepository( basedir.getAbsoluteFile() );

        if ( dumpFile != null )
        {
            InputStream is = new FileInputStream( dumpFile );
            loadSvnRepository( is, basedir.getAbsoluteFile() );
        }

        if ( confDir != null )
        {
            copyDir( confDir, new File( basedir, "conf" ) );
        }

        svnPort = ( port == 0 ) ? getUnusedPort() : port;

        server = ( svnPort > 0 ) ? svnserve( basedir, svnPort ) : null;

        return this;
    }

    public static void createSvnRepository( File dir )
        throws IOException
    {
        svnadmin( null, "create", dir.getAbsolutePath() );
    }

    public static void loadSvnRepository( InputStream dumpStream, File basedir )
        throws IOException
    {
        try
        {
            svnadmin( dumpStream, "load", basedir.getAbsolutePath() );
        }
        finally
        {
            close( dumpStream );
        }
    }

    private static void copyDir( File srcDir, File dstDir )
        throws IOException
    {
        if ( !srcDir.exists() )
        {
            throw new FileNotFoundException( srcDir.getAbsolutePath() );
        }
        if ( !srcDir.isDirectory() )
        {
            throw new IOException( srcDir.getAbsolutePath() + " is not a directory" );
        }
        dstDir.mkdirs();
        for ( String file : srcDir.list() )
        {
            File srcFile = new File( srcDir, file );
            if ( srcFile.isFile() )
            {
                File dstFile = new File( dstDir, file );
                copyFile( srcFile, dstFile );
            }
        }
    }

    private static void copyFile( File srcFile, File dstFile )
        throws IOException
    {
        FileInputStream is = new FileInputStream( srcFile );
        try
        {
            FileOutputStream os = new FileOutputStream( dstFile );
            try
            {
                copy( is, os, 1024 * 4 );
            }
            finally
            {
                os.close();
            }
        }
        finally
        {
            is.close();
        }
    }

    private static void copy( InputStream is, OutputStream os, int bufferSize )
        throws IOException
    {
        for ( byte[] buffer = new byte[bufferSize];; )
        {
            int n = is.read( buffer );
            if ( n < 0 )
            {
                break;
            }
            os.write( buffer, 0, n );
            os.flush();
        }
    }

    private static void close( InputStream is )
    {
        try
        {
            is.close();
        }
        catch ( IOException e )
        {
            // ignored
        }
    }

    private static void close( OutputStream os )
    {
        try
        {
            os.close();
        }
        catch ( IOException e )
        {
            // ignored
        }
    }

    private static void svnadmin( InputStream is, String... args )
        throws IOException
    {
        ProcessBuilder procBuilder = new ProcessBuilder( getSvnAdminCmd() );
        procBuilder.command().addAll( Arrays.asList( args ) );
        Process proc = procBuilder.start();
        if ( is != null )
        {
            new StreamPumper( is, proc.getOutputStream(), true ).start();
        }
        else
        {
            close( proc.getOutputStream() );
        }
        new StreamPumper( proc.getInputStream(), System.out, false ).start();
        new StreamPumper( proc.getErrorStream(), System.err, false ).start();
        try
        {
            int result = proc.waitFor();
            if ( result != 0 )
            {
                throw new IOException( "Execution of svnadmin failed with code " + result );
            }
        }
        catch ( InterruptedException e )
        {
            throw new IOException( "Execution of svnadmin was interrupted" );
        }
        finally
        {
            close( proc.getOutputStream() );
        }
    }

    private Process svnserve( File basedir, int port )
        throws IOException
    {
        ProcessBuilder procBuilder = new ProcessBuilder( getSvnServeCmd() );
        procBuilder.command().add( "--daemon" );
        procBuilder.command().add( "--foreground" );
        procBuilder.command().add( "--listen-port" );
        procBuilder.command().add( Integer.toString( port ) );
        procBuilder.command().add( "--root" );
        procBuilder.command().add( basedir.getAbsolutePath() );

        Process proc = procBuilder.start();
        close( proc.getOutputStream() );
        new StreamPumper( proc.getInputStream(), System.out, false ).start();
        new StreamPumper( proc.getErrorStream(), System.err, false ).start();

        return proc;
    }

    private int getUnusedPort()
    {
        int port;

        try
        {
            Socket socket = new Socket();
            socket.bind( null );
            port = socket.getLocalPort();
            socket.close();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            port = DEFAULT_PORT;
        }

        return port;
    }

    private static String getSvnAdminCmd()
    {
        return "svnadmin";
    }

    private String getSvnServeCmd()
    {
        return "svnserve";
    }

    /**
     * Stops the server. Stopping an already stopped server has no effect.
     */
    public void stop()
    {
        if ( server != null )
        {
            server.destroy();
            server = null;
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e )
            {
                // ignored
            }
        }

        if ( basedir != null )
        {
            delete( basedir );
            basedir = null;
        }
    }

    private void delete( File file )
    {
        File[] children = file.listFiles();
        if ( children != null )
        {
            for ( File child : children )
            {
                delete( child );
            }
        }
        file.delete();
    }

    static class StreamPumper
        extends Thread
    {

        private final InputStream is;

        private final OutputStream os;

        private final boolean closeOutput;

        public StreamPumper( InputStream is, OutputStream os, boolean closeOutput )
        {
            this.is = is;
            this.os = os;
            this.closeOutput = closeOutput;
            setDaemon( true );
        }

        @Override
        public void run()
        {
            try
            {
                copy( is, os, 1024 );
            }
            catch ( IOException e )
            {
                // ignore
            }
            finally
            {
                close( is );
                if ( closeOutput )
                {
                    close( os );
                }
            }
        }

    }

}

/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.tests.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

/**
 * A helper for the tests to start a SSH server. Create an instance of this class, use its mutators to configure the
 * server and finally call {@link #start()}.
 * 
 * @author Benjamin Bentmann
 */
public class SshServer
{

    private org.apache.sshd.SshServer server;

    private int port;

    private final Map<String, String> userPasswords = new HashMap<String, String>();

    /**
     * Sets the port to start the server on.
     * 
     * @param port The port to start the server on, may be {@code 0} to pick a random unused port.
     * @return This server, never {@code null}.
     */
    public SshServer setPort( int port )
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
            return server.getPort();
        }
        else
        {
            return port;
        }
    }

    /**
     * Gets the base URL to the server's connector, e.g. {@code "ssh://localhost:22"}.
     * 
     * @return The base URL without trailing slash to the server's connector, never {@code null}.
     */
    public String getUrl()
    {
        return "ssh://localhost:" + getPort();
    }

    /**
     * Registers a user.
     * 
     * @param username The username, must not be {@code null}.
     * @param password The password, must not be {@code null}.
     * @return This server, never {@code null}.
     */
    public SshServer addUser( String username, String password )
    {
        userPasswords.put( username, password );

        return this;
    }

    /**
     * Starts the server. Trying to start an already running server has no effect.
     * 
     * @throws Exception If the server could not be started.
     * @return This server, never {@code null}.
     */
    public SshServer start()
        throws Exception
    {
        if ( server != null )
        {
            return this;
        }

        server = org.apache.sshd.SshServer.setUpDefaultServer();
        server.setPort( port );
        server.setKeyPairProvider( new SimpleGeneratorHostKeyProvider() );
        server.setPasswordAuthenticator( new PasswordAuthenticator()
        {
            public boolean authenticate( String username, String password, ServerSession session )
            {
                return password != null && password.equals( userPasswords.get( username ) );
            }
        } );
        server.setCommandFactory( new CommandFactory()
        {
            public Command createCommand( String command )
            {
                /*
                 * NOTE: It would have been nice to just use o.a.s.s.s.ProcessShellFactory but that has a bug in its
                 * stream pumping that makes it miss to transmit the last bytes from the pipes when the command has
                 * terminated.
                 */
                return new ExternalCommand( command );
            }
        } );
        final UserAuth anonAuth = new UserAuth()
        {
            public Boolean auth( ServerSession session, String username, Buffer buffer )
                throws Exception
            {
                return "".equals( userPasswords.get( username ) );
            }
        };
        List<NamedFactory<UserAuth>> authFactories = new ArrayList<NamedFactory<UserAuth>>();
        authFactories.addAll( server.getUserAuthFactories() );
        authFactories.add( new NamedFactory<UserAuth>()
        {
            public String getName()
            {
                return "none";
            }

            public UserAuth create()
            {
                return anonAuth;
            }
        } );
        server.setUserAuthFactories( authFactories );
        server.start();

        return this;
    }

    private static String[] splitCommand( String command )
    {
        List<String> cmds = new ArrayList<String>();

        StringBuilder buffer = new StringBuilder( command.length() );

        char delim = ' ';
        for ( int i = 0; i < command.length(); i++ )
        {
            char c = command.charAt( i );
            if ( c == delim )
            {
                cmds.add( buffer.toString() );
                buffer.setLength( 0 );
                delim = ' ';
            }
            else if ( delim == ' ' && ( c == '\"' || c == '\'' ) )
            {
                delim = c;
            }
            else
            {
                buffer.append( c );
            }
        }

        if ( buffer.length() > 0 )
        {
            cmds.add( buffer.toString() );
        }

        return cmds.toArray( new String[cmds.size()] );
    }

    /**
     * Stops the server. Stopping an already stopped server has no effect.
     */
    public void stop()
    {
        if ( server != null )
        {
            try
            {
                server.stop();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            server = null;
        }
    }

    private static class ExternalCommand
        implements Command
    {

        private String[] cmds;

        private Process process;

        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private ExitCallback exitCallback;

        public ExternalCommand( String command )
        {
            cmds = splitCommand( command );
        }

        public void start( Environment env )
            throws IOException
        {
            ProcessBuilder procBuilder = new ProcessBuilder( cmds );
            if ( env != null && env.getEnv() != null )
            {
                procBuilder.environment().putAll( env.getEnv() );
            }
            process = procBuilder.start();
            CountDownLatch latch = new CountDownLatch( 2 );
            new StreamPumper( in, process.getOutputStream(), true, null ).start();
            new StreamPumper( process.getInputStream(), out, false, latch ).start();
            new StreamPumper( process.getErrorStream(), err, false, latch ).start();
            new ExitNotifier( exitCallback, process, latch ).start();
        }

        public void destroy()
        {
            if ( process != null )
            {
                process.destroy();
                process = null;
            }
        }

        public void setInputStream( InputStream in )
        {
            this.in = in;
        }

        public void setOutputStream( OutputStream out )
        {
            this.out = out;
        }

        public void setErrorStream( OutputStream err )
        {
            this.err = err;
        }

        public void setExitCallback( ExitCallback callback )
        {
            this.exitCallback = callback;
        }

    }

    private static class ExitNotifier
        extends Thread
    {

        private final ExitCallback callback;

        private final Process process;

        private final CountDownLatch latch;

        public ExitNotifier( ExitCallback callback, Process process, CountDownLatch latch )
        {
            this.callback = callback;
            this.process = process;
            this.latch = latch;
            setDaemon( true );
        }

        @Override
        public void run()
        {
            try
            {
                process.waitFor();
                latch.await();
                callback.onExit( process.exitValue() );
            }
            catch ( Exception e )
            {
                callback.onExit( 1 );
            }
        }

    }

    private static class StreamPumper
        extends Thread
    {

        private final InputStream is;

        private final OutputStream os;

        private final boolean closeOutput;

        private final CountDownLatch latch;

        public StreamPumper( InputStream is, OutputStream os, boolean closeOutput, CountDownLatch latch )
        {
            this.is = is;
            this.os = os;
            this.closeOutput = closeOutput;
            this.latch = latch;
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
                if ( latch != null )
                {
                    latch.countDown();
                }
                close( is );
                if ( closeOutput )
                {
                    close( os );
                }
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

    }

}

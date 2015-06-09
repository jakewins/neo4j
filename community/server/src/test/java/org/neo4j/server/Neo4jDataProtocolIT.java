/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.neo4j.server.configuration.ServerSettings;
import org.neo4j.test.server.ExclusiveServerTestBase;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.neo4j.server.helpers.CommunityServerBuilder.server;

public class Neo4jDataProtocolIT extends ExclusiveServerTestBase
{
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private CommunityNeoServer server;
    private static SelfSignedCertificate ssc;

    @BeforeClass
    public static void setup() throws CertificateException
    {
        ssc = new SelfSignedCertificate();
    }

    @After
    public void stopTheServer()
    {
        server.stop();
    }

    @Test
    public void shouldLaunchNDP() throws Throwable
    {
        // When I run Neo4j with NDP enabled
        server = server()
                .withProperty( ServerSettings.tls_key_file.name(), ssc.privateKey().getAbsolutePath() )
                .withProperty( ServerSettings.tls_certificate_file.name(), ssc.certificate().getAbsolutePath() )
                .withProperty( ServerSettings.ndp_enabled.name(), "true" )
                .usingDatabaseDir( tmpDir.getRoot().getAbsolutePath() )
                .build();
        server.start();

        // Then
        assertEventuallyServerResponds( "localhost", 7687 );
    }

    @Test
    public void shouldBeAbleToSpecifyHostAndPort() throws Throwable
    {
        // When I run Neo4j with the ndp extension on the class path
        // When I run Neo4j with NDP enabled
        server = server()
                .withProperty( ServerSettings.tls_key_file.name(), ssc.privateKey().getAbsolutePath() )
                .withProperty( ServerSettings.tls_certificate_file.name(), ssc.certificate().getAbsolutePath() )
                .withProperty( ServerSettings.ndp_enabled.name(), "true" )
                .withProperty( ServerSettings.ndp_socket_address.name(), "localhost:8776" )
                .usingDatabaseDir( tmpDir.getRoot().getAbsolutePath() )
                .build();
        server.start();

        // Then
        assertEventuallyServerResponds( "localhost", 8776 );
    }

    @Test
    public void shouldFailSslHandshake() throws Throwable
    {
        // When I create a server and waiting for connection
        server = server()
                .withProperty( ServerSettings.tls_key_file.name(), ssc.privateKey().getAbsolutePath() )
                .withProperty( ServerSettings.tls_certificate_file.name(), ssc.certificate().getAbsolutePath() )
                .withProperty( ServerSettings.ndp_enabled.name(), "true" )
                .withProperty( ServerSettings.ndp_socket_address.name(), "localhost:8776" )
                .usingDatabaseDir( tmpDir.getRoot().getAbsolutePath() )
                .build();
        server.start();

        // Then
        try( Socket socket = new Socket() )
        {
            socket.setSoTimeout( 30_000 );
            socket.connect( new InetSocketAddress( "localhost", 8776 ) );
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // some random data to fail ssl handshake
            out.write( new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 14, 15} );

            byte[] accepted = {-1, -1, -1, -1}; // some value except 0, 0, 0, 0
            int read = in.read( accepted );

            assertEquals( 4, read );
            assertArrayEquals( new byte[]{0, 0, 0, 0}, accepted );
        }

    }

    private void assertEventuallyServerResponds( String host, int port )
            throws IOException, NoSuchAlgorithmException, KeyManagementException
    {
        try
        {
            SSLContext context = SSLContext.getInstance( "SSL" );
            context.init( new KeyManager[0], new TrustManager[]{new X509TrustManager()
            {
                @Override
                public void checkClientTrusted( X509Certificate[] x509Certificates, String s )
                        throws CertificateException
                {
                }

                @Override
                public void checkServerTrusted( X509Certificate[] x509Certificates, String s )
                        throws CertificateException
                {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }
            }}, new SecureRandom() );

            try ( Socket socket = context.getSocketFactory().createSocket(); )
            {
                // Ok, we can connect - can we perform the version handshake?
                socket.setSoTimeout( 30_000 );
                socket.connect( new InetSocketAddress( host, port ) );
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                // Hard-coded handshake, a general "test client" would be useful further on.
                out.write( new byte[]{0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0} );

                byte[] accepted = new byte[4];
                in.read( accepted );

                assertArrayEquals( new byte[]{0, 0, 0, 1}, accepted );
            }
        }
        catch ( SocketTimeoutException e )
        {
            throw new RuntimeException( "Waited for 30 seconds for server to respond to HTTP calls, " +
                                        "but no response, timing out to avoid blocking forever." );
        }
    }
}

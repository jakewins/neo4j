package org.neo4j.shell.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

public class HostBoundSocketFactory implements RMIClientSocketFactory, RMIServerSocketFactory
{
    private final InetAddress inetAddress;

    public HostBoundSocketFactory( String host ) throws UnknownHostException
    {
        this.inetAddress = InetAddress.getByName( host );
    }

    @Override
    public Socket createSocket( String host, int port ) throws IOException
    {
        return new Socket( host, port );
    }

    @Override
    public ServerSocket createServerSocket( int port ) throws IOException
    {
        return new ServerSocket( port, 50, inetAddress );
    }
}

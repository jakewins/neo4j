/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.tools.boltalyzer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.tools.boltalyzer.pcap.PCAPParser;
import org.neo4j.tools.boltalyzer.pcap.Packet;

public class Boltalyzer
{
    public static void main(String ... args) throws IOException
    {
        if ( args.length != 1 )
        {
            System.out.println( "USAGE: boltalyzer <PCAP FILE>" );
            System.exit( 0 );
        }


        int serverPort = 7687;

        try ( FileInputStream pcap = new FileInputStream( args[0] ) )
        {
            new PCAPParser().parse( pcap )
                    .map( new PacketToExchange( serverPort ) )
                    .forEach( System.out::println );
        }
    }

    public static class SessionRepository
    {
        private final Map<String,AnalyzedSession> openSessions = new HashMap<>();
        private int sessionCount = 0;

        public AnalyzedSession session( String clientAddress )
        {
            AnalyzedSession session = openSessions.get( clientAddress );
            if( session == null )
            {
                session = new AnalyzedSession( String.format("session-%03d", sessionCount++) );
                openSessions.put( clientAddress, session );
            }
            return session;
        }
    }

    private static class PacketToExchange implements Function<Packet,Object>
    {
        private final SessionRepository sessions = new SessionRepository();
        private final int serverPort;
        private long startTime = -1;

        public PacketToExchange( int serverPort )
        {
            this.serverPort = serverPort;
        }

        @Override
        public Object apply( Packet packet )
        {
            try
            {
                if ( startTime == -1 )
                {
                    startTime = packet.timestamp();
                }

                String source;
                AnalyzedSession session;
                String description;
                if ( packet.dstPort() == serverPort )
                {
                    source = "Client";
                    session = sessions.session( packet.src().toString() + packet.srcPort() );
                    description = session.describeClientPayload( packet );
                }
                else
                {
                    source = "Server";
                    session = sessions.session( packet.dst().toString() + packet.dstPort() );
                    description = session.describeServerPayload( packet );
                }

                long deltaTime = packet.timestamp() - startTime;

                return String.format( "%08d %s %s%s", deltaTime, session.name(), source, description );
            }
            catch( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }
}

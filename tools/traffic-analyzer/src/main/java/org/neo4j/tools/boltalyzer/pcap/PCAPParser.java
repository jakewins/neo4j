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
package org.neo4j.tools.boltalyzer.pcap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.neo4j.function.ThrowingSupplier;


public class PCAPParser
{
    private final int PCAP_HEADER = 0xA1B2C3D4;

    private static final Map<Integer,PhysicalFormat> physicalFormats = new HashMap<>();

    /** tcpdump supports lots of network types. We implement them as a lambda that takes a tcpdump raw frame strips off the physical transport frame */
    interface PhysicalFormat
    {
        /**
         * Given a stream currently pointing to the beginning of a frame (eg. localhost, ethernet or similar), strip off the physical transport and return
         * the inner data.
         */
        byte[] read( LittleEndianStream in, int length ) throws IOException;
    }

    static
    {
        // http://www.tcpdump.org/linktypes.html

        // BSD Loopback
        physicalFormats.put( 0, ( in, length ) -> {
            in.skip( 4 );
            return in.read( length - 4 );
        } );
    }


    public Stream<Packet> parse( InputStream rawStream ) throws IOException
    {
        LittleEndianStream in = new LittleEndianStream( rawStream );
        // Valid PCAP file starts with a 32-bit integer header
        int i = in.readInt();
        if( i != PCAP_HEADER )
        {
            throw new IOException( "Provided file is not a valid PCAP dump, valid dump files should start with 0x" +
                                   Integer.toHexString( PCAP_HEADER ) + "." );
        }

        // Followed by 16 header bytes I didn't care to look up
        in.skip(16);

        // Followed by the network type
        int networkType = in.readInt();
        PhysicalFormat physicalFormat = physicalFormats.get( networkType );
        if( networkType != 0 )
        {
            throw new IOException( "Don't know how do decode packets from " + Integer.toHexString( networkType ) + " network type. You need to add a physical format parser for this format to PCAPParser." );
        }

        return streamFrom( () -> {

            if( !in.hasMore() )
            {
                return null;
            }

            // PCAP Packet Header: [int32 seconds][int32 ms][int32 frame captured size][int32 actual frame size]
            long timestampSeconds = in.readInt();
            long timestampMilliseconds = in.readInt();
            int packetSize = in.readInt();
            int actualPacketSize = in.readInt();

            long timestamp = timestampSeconds * 1000 + timestampMilliseconds / 1000;
            if( packetSize != actualPacketSize )
            {
                return null;
            }

            if( packetSize == 0 )
            {
                return null;
            }

            // Read the packet, unwrapped from the physical layer wrapping
            byte[] rawPacket = physicalFormat.read( in, packetSize );

            return new Packet( rawPacket, timestamp );
        });
    }

    private Stream<Packet> streamFrom( ThrowingSupplier<Packet, IOException> supplier ) throws IOException
    {
        return StreamSupport.stream( Spliterators.spliteratorUnknownSize( new Iterator<Packet>()
        {
            private Packet next = supplier.get();

            @Override
            public boolean hasNext()
            {
                return next != null;
            }

            @Override
            public Packet next()
            {
                try
                {
                    Packet current = next;
                    next = supplier.get();
                    return current;
                }
                catch( IOException e )
                {
                    throw new RuntimeException( e );
                }
            }
        }, Spliterator.IMMUTABLE ), false );
    }

    private static class LittleEndianStream
    {
        private final InputStream stream;
        private final byte[] intBuffer = new byte[4];
        private final ByteBuffer littleEndianBufferView = ByteBuffer.wrap( intBuffer ).order( ByteOrder.LITTLE_ENDIAN );

        public LittleEndianStream( InputStream stream )
        {
            this.stream = stream;
        }

        private int readInt() throws IOException
        {
            // PCAP is litte-endian, so we need our own int reading method
            int read = stream.read( intBuffer );
            if ( read != 4 )
            {
                throw new IOException( "Expected at least 4 bytes left to read an integer, found " + read );
            }
            littleEndianBufferView.clear();
            return littleEndianBufferView.getInt();
        }

        public void skip( int numBytes ) throws IOException
        {
            if( stream.skip( numBytes ) != numBytes )
            {
                throw new IOException( "EOF" );
            }
        }

        public boolean hasMore() throws IOException
        {
            return stream.available() > 0;
        }

        public byte[] read( int size ) throws IOException
        {
            byte[] data = new byte[size];
            if( stream.read(data) != data.length )
            {
                throw new IOException( "EOF" );
            }
            return data;
        }
    }
}

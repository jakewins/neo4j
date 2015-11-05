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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * For simplicity for now, combined representation of IP+TCP packet.
 * Provides a view of a byte[] as a TCP packet wrapped in an IP packet.
 */
public class Packet
{
    public static final int IP_PROTOCOL_OFFSET = 9;
    public static final int IP_VERSION_AND_LENGTH_OFFSET = 0;
    public static final int IP_SRC_OFFSET = 12;
    public static final int IP_DST_OFFSET = 16;

    public static final int TCP_PROTOCOL = 6;

    public static final int TCP_SRC_PORT_OFFSET = 0;
    public static final int TCP_DST_PORT_OFFSET = 2;

    /** Position in the raw data where TCP packet starts */
    private final int tcpPacketOffset;

    /** Position in the raw data where TCP payload data starts */
    private final int tcpPayloadOffset;

    private final InetAddress src;
    private final InetAddress dst;

    private final int dstPort;
    private final int srcPort;
    private final long timestamp;

    private final byte[] raw;

    public Packet( byte[] raw, long timestamp ) throws UnknownHostException
    {
        this.raw = raw;
        this.timestamp = timestamp;

        if( raw.length > 0 )
        {
            this.tcpPacketOffset = ipHeaderLength();
            this.tcpPayloadOffset = tcpPacketOffset + tcpHeaderLength();

            this.src = parseInetAddress( IP_SRC_OFFSET, raw );
            this.dst = parseInetAddress( IP_DST_OFFSET, raw );

            this.srcPort = readPort( TCP_SRC_PORT_OFFSET );
            this.dstPort = readPort( TCP_DST_PORT_OFFSET );
        }
        else {
            this.tcpPacketOffset = 0;
            this.tcpPayloadOffset = 0;

            this.src = null;
            this.dst = null;

            this.srcPort = 0;
            this.dstPort = 0;
        }
    }

    public static InetAddress parseInetAddress(int offset, byte[] raw) throws UnknownHostException
    {
        byte[] ipRaw = new byte[4];
        System.arraycopy( raw, offset, ipRaw, 0, ipRaw.length );
        return InetAddress.getByAddress( ipRaw );
    }

    public long timestamp()
    {
        return timestamp;
    }

    public int ipHeaderLength()
    {
        return (raw[IP_VERSION_AND_LENGTH_OFFSET] & 0xF) * 4;
    }

    public int tcpHeaderLength(){
        return ((raw[tcpPacketOffset + 12] >> 4) & 0xF) * 4;
    }

    public InetAddress dst()
    {
        return dst;
    }

    public InetAddress src()
    {
        return src;
    }

    public int dstPort()
    {
        return dstPort;
    }

    public int srcPort()
    {
        return srcPort;
    }

    public ByteBuffer payload()
    {
        return ByteBuffer.wrap( raw, tcpPayloadOffset, raw.length - tcpPayloadOffset );
    }

    private int readPort( int offset )
    {
        offset = offset + tcpPacketOffset;
        return ((raw[offset] & 0xFF) << 8) | (raw[offset + 1] & 0xFF);
    }

    @Override
    public String toString()
    {
        return "Packet{" +
               "timestamp=" + timestamp +
               ", src=" + src +
               ", dst=" + dst +
               ", dstPort=" + dstPort +
               ", srcPort=" + srcPort +
               ", size=" + payloadLength() +
               '}';
    }

    public int payloadLength()
    {
        return raw.length - tcpPayloadOffset;
    }
}

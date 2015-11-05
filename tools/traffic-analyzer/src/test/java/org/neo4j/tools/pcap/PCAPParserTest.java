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
package org.neo4j.tools.pcap;

import org.junit.Test;

import java.io.DataInputStream;
import java.util.List;

import org.neo4j.tools.boltalyzer.pcap.PCAPParser;
import org.neo4j.tools.boltalyzer.pcap.Packet;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PCAPParserTest
{

    @Test
    public void shouldParseBasicCaptureFile() throws Throwable
    {
        // Given
        DataInputStream pcapStream = new DataInputStream( getClass().getClassLoader().getResourceAsStream( "BSDLocalhost.pcap" ) );

        // When
        List<Packet> packets = new PCAPParser().parse( pcapStream ).collect( toList() );

        // Then
        assertThat( packets.size(), equalTo( 21 ));
        assertThat( packets.get( 0 ).dstPort(), equalTo( 7687 ));
        assertThat( packets.get( 0 ).srcPort(), equalTo( 49349 ));
    }

}

/**
 * Copyright (c) 2002-2014 "Neo Technology,"
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
package org.neo4j.kernel.impl.util.collection;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class LongHashMapTest
{
    @Test
    public void shouldGetAndContain() throws Exception
    {
        // Given
        LongObjectOpenHashMap<Object> map = new LongObjectOpenHashMap<>();

        // When
        map.put( 22, true );

        // Then
        assertThat(map.containsKey( 22 ), equalTo(true));
        assertThat(map.containsKey( 21 ), equalTo(false));
        assertThat(map.containsKey( 23 ), equalTo(false));

        assertThat(map.get( 22 ), equalTo((Object)true));
    }

    @Test
    public void shouldReplace() throws Exception
    {
        // Given
        LongObjectOpenHashMap<Object> map = new LongObjectOpenHashMap<>();
        map.put( 22, true );

        // When
        map.put( 22, false );

        // Then
        assertThat(map.containsKey( 22 ), equalTo(true));
        assertThat(map.containsKey( 21 ), equalTo(false));
        assertThat(map.containsKey( 23 ), equalTo(false));

        assertThat(map.get( 22 ), equalTo((Object)false));
    }

    @Test
    public void shouldRemove() throws Exception
    {
        // Given
        LongObjectOpenHashMap<Object> map = new LongObjectOpenHashMap<>();
        map.put( 22, true );

        // When
        map.getAndRemove( 22 );

        // Then
        assertThat(map.containsKey( 22 ), equalTo(false));
        assertThat(map.containsKey( 21 ), equalTo(false));
        assertThat(map.containsKey( 23 ), equalTo(false));

        assertThat(map.get( 22 ), equalTo(null));
    }

    @Test
    public void shouldHandleResizing() throws Exception
    {
        // Given
        LongObjectOpenHashMap<Object> map = new LongObjectOpenHashMap<>( 16 );

        // When
        for ( int i = 0; i < 128; i++ )
        {
            map.put( i, i );
        }

        // Then
        assertThat( map.size(), equalTo(128) );

        int count = 0;
        for ( int i = 0; i < 128; i++ )
        {
            assertThat(map.get(i), equalTo((Object)i));
            count++;
        }
        assertThat(count, equalTo(128));
    }
}

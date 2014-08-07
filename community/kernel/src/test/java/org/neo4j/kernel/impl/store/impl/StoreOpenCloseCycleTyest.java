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
package org.neo4j.kernel.impl.store.impl;

import java.io.File;

import org.junit.Test;
import org.neo4j.io.fs.FileLock;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.fs.StoreChannel;
import org.neo4j.kernel.impl.util.StringLogger;

import static org.mockito.Mockito.*;

public class StoreOpenCloseCycleTyest
{
    @Test
    public void shouldLockAndUnlock() throws Exception
    {
        // Given
        StoreFormat<?,?> format = mock(StoreFormat.class);
        when(format.version()).thenReturn( "v1.0.0" );
        when(format.type()).thenReturn( "SomeFormat" );

        File dbFileName = new File( "/store" );
        StoreChannel channel = mock(StoreChannel.class);

        FileLock lock = mock(FileLock.class);

        FileSystemAbstraction fs = mock(FileSystemAbstraction.class);
        when(fs.tryLock( dbFileName, channel )).thenReturn( lock );


        StoreOpenCloseCycle logic = new StoreOpenCloseCycle( StringLogger.DEV_NULL,
                dbFileName, format, fs );

        // When
        logic.openStore(channel);

        // Then
        verify( fs ).tryLock(dbFileName, channel );

        // And when
        logic.closeStore( channel, 0 );

        // Then
        verify( lock ).release();
    }
}

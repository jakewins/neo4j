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
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.io.fs.StoreChannel;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.io.pagecache.impl.standard.StandardPageCache;
import org.neo4j.kernel.impl.nioneo.store.NotCurrentStoreVersionException;
import org.neo4j.kernel.impl.store.Store;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.test.EphemeralFileSystemRule;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class StandardStoreVersioningTest
{
    @Rule
    public EphemeralFileSystemRule fsRule = new EphemeralFileSystemRule();

    private StandardPageCache pageCache;

    @Before
    public void setup()
    {
        pageCache = new StandardPageCache( fsRule.get(), 1024, 1024 );
    }

    @Test
    public void shouldThrowExceptionOnMismatchingVersion() throws Throwable
    {
        // Given
        Store<?, ?> onePointOhStore = new StandardStore<>( new VersionedStoreFormat("v0.1.0"),
                new File("/store"), new TestStoreIdGenerator(), pageCache, fsRule.get(), StringLogger.DEV_NULL );

        Store<?, ?> twoPointOhStore = new StandardStore<>( new VersionedStoreFormat("v0.2.0"),
                new File("/store"), new TestStoreIdGenerator(), pageCache, fsRule.get(), StringLogger.DEV_NULL );

        // A store with an older version
        onePointOhStore.init();
        onePointOhStore.shutdown();

        // When
        try
        {
            twoPointOhStore.init();
            fail("Should not have opened this store.");
        } catch( NotCurrentStoreVersionException e )
        {
            // Then
            assertThat( e.getMessage(), equalTo( "Was expecting store version [VersionedStoreFormatv0.2.0] " +
                    "but found [VersionedStoreFormatv0.1.0]. Store cannot be upgraded automatically. " ));
        }

    }
}

class VersionedStoreFormat implements StoreFormat<Object, Store.RecordCursor<Object>>
{
    private final String version;

    public VersionedStoreFormat( String version )
    {
        this.version = version;
    }

    @Override
    public Store.RecordCursor<Object> createCursor( PagedFile file, StoreToolkit toolkit )
    {
        return null;
    }

    @Override
    public RecordFormat<Object> recordFormat()
    {
        return null;
    }

    @Override
    public String version()
    {
        return version;
    }

    @Override
    public String type()
    {
        return "VersionedStoreFormat";
    }

    @Override
    public int headerSize()
    {
        return 0;
    }

    @Override
    public int recordSize( StoreChannel channel ) throws IOException
    {
        return 8;
    }

    @Override
    public void createStore( StoreChannel channel ) throws IOException
    {

    }
}

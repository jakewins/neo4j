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
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.io.fs.StoreChannel;
import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.io.pagecache.impl.standard.StandardPageCache;
import org.neo4j.kernel.impl.store.Store;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.lifecycle.LifeSupport;
import org.neo4j.test.EphemeralFileSystemRule;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.neo4j.kernel.impl.store.impl.StoreFormat.RecordFormat;

public class StandardStoreTest
{
    @Rule
    public EphemeralFileSystemRule fsRule = new EphemeralFileSystemRule();

    private LifeSupport life;
    private StandardPageCache pageCache;

    @Before
    public void setup()
    {
        life = new LifeSupport();
        life.start();
        pageCache = new StandardPageCache( fsRule.get(), 1024, 1024 );
    }

    @Test
    public void shouldReadRecords() throws Throwable
    {
        // Given
        Store<MyRecord, MyCursor> store = life.add(new StandardStore<>( new MyHeaderlessStoreFormat(), new File("/store"),
                new TestStoreIdGenerator(), pageCache, fsRule.get(), StringLogger.DEV_NULL ));

        long firstId = store.allocate();
        long secondId = store.allocate();

        store.write( new MyRecord( firstId, 1337) );
        store.write( new MyRecord( secondId, 1338) );

        // When
        MyRecord firstRecord = store.read( firstId );
        MyRecord secondRecord = store.read( secondId );

        // Then
        assertThat(firstRecord.value, equalTo(1337l));
        assertThat(secondRecord.value, equalTo(1338l));

        // And when I restart the store
        life.shutdown();

        store = new StandardStore<>( new MyHeaderlessStoreFormat(), new File("/store"),
                new TestStoreIdGenerator(), pageCache, fsRule.get(), StringLogger.DEV_NULL );
        store.init();
        store.start();

        assertThat( StoreMatchers.records( store ),
           equalTo( asList( new MyRecord( firstId, 1337 ), new MyRecord( secondId, 1338 ) ) ) );
    }

    @Test
    public void shouldAllowStoresWithHeaders() throws Throwable
    {
        // Given
        Store<MyRecord, MyCursor> store = life.add(new StandardStore<>( new MyFormatWithHeader(14), new File("/store"),
                new TestStoreIdGenerator(), pageCache, fsRule.get(), StringLogger.DEV_NULL ));

        long recordId = store.allocate();

        store.write( new MyRecord( recordId, 1338) );

        // When
        MyRecord secondRecord = store.read( recordId );

        // Then
        assertThat(secondRecord.value, equalTo(1338l));

        // And when I restart the store
        life.shutdown();

        store = new StandardStore<>( new MyFormatWithHeader(14), new File("/store"),
                new TestStoreIdGenerator(), pageCache, fsRule.get(), StringLogger.DEV_NULL );
        store.init();
        store.start();

        assertThat( StoreMatchers.records( store ), equalTo( asList( new MyRecord( recordId, 1338 ) ) ) );
    }
}


class MyCursor extends BaseRecordCursor<MyRecord>
{
    MyCursor( PagedFile file, StoreToolkit toolkit, MyRecordFormat format )
    {
        super(file, toolkit, format);
    }
}

class MyRecordFormat implements RecordFormat<MyRecord>
{
    @Override
    public String recordName()
    {
        return "MyRecord";
    }

    @Override
    public long id( MyRecord myRecord )
    {
        return myRecord.id;
    }

    @Override
    public void serialize( PageCursor cursor, int offset, MyRecord myRecord )
    {
        cursor.putLong(offset, myRecord.value);
    }

    @Override
    public MyRecord deserialize( PageCursor cursor, int offset, long id )
    {
        return new MyRecord( id, cursor.getLong(offset) );
    }

    @Override
    public boolean inUse( PageCursor pageCursor, int offset )
    {
        return pageCursor.getLong(offset) != 0;
    }
}

class MyHeaderlessStoreFormat extends FixedSizeRecordStoreFormat<MyRecord, MyCursor>
{
    private final MyRecordFormat recordFormat;

    public MyHeaderlessStoreFormat()
    {
        super( 8, "HeaderlessFormat", "v0.1.0" );
        this.recordFormat = new MyRecordFormat();
    }

    @Override
    public MyCursor createCursor( PagedFile file, StoreToolkit toolkit )
    {
        return new MyCursor( file, toolkit, recordFormat );
    }

    @Override
    public RecordFormat<MyRecord> recordFormat()
    {
        return recordFormat;
    }
}

class MyFormatWithHeader implements StoreFormat<MyRecord, MyCursor>
{
    private final int configuredRecordSize;
    private final MyRecordFormat recordFormat;

    MyFormatWithHeader( int configuredRecordSize )
    {
        this.configuredRecordSize = configuredRecordSize;
        this.recordFormat = new MyRecordFormat();
    }

    @Override
    public MyCursor createCursor( PagedFile file, StoreToolkit toolkit )
    {
        return new MyCursor( file, toolkit, recordFormat );
    }

    @Override
    public RecordFormat<MyRecord> recordFormat()
    {
        return recordFormat;
    }

    @Override
    public int recordSize( StoreChannel channel ) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate( 4 );
        channel.read( buf, 0 );
        buf.flip();
        return buf.getInt();
    }

    @Override
    public void createStore( StoreChannel channel ) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate( 4 );
        buf.putInt( configuredRecordSize );
        buf.flip();
        channel.write( buf, 0 );
    }

    @Override
    public int headerSize()
    {
        return 4;
    }

    @Override
    public String version()
    {
        return "v0.1.0";
    }

    @Override
    public String type()
    {
        return "MyFormatWithHeader";
    }
}

class MyRecord
{
    long id;
    long value;

    public MyRecord(long id, long value)
    {
        this.id = id;
        this.value = value;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        MyRecord myRecord = (MyRecord) o;

        if ( id != myRecord.id )
        {
            return false;
        }
        if ( value != myRecord.value )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (value ^ (value >>> 32));
        return result;
    }

    @Override
    public String toString()
    {
        return "MyRecord{" +
                "id=" + id +
                ", value=" + value +
                '}';
    }
}
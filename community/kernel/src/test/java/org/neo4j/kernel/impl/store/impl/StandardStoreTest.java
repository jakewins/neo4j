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
import org.neo4j.kernel.lifecycle.LifeSupport;
import org.neo4j.test.EphemeralFileSystemRule;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.neo4j.io.pagecache.PagedFile.PF_SHARED_LOCK;

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
    public void shouldReadRecords() throws Exception
    {
        // Given
        Store<MyRecord, MyCursor> store = life.add(new StandardStore<>( new MyHeaderLessFormat(), new File("/store"),
                new TestStoreIdGenerator(), pageCache, fsRule.get() ));

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

        assertThat( StoreMatchers.records( store ),
           equalTo( asList( new MyRecord( firstId, 1337 ), new MyRecord( secondId, 1338 ) ) ) );
    }

    @Test
    public void shouldAllowStoresWithHeaders() throws Throwable
    {
        // Given
        Store<MyRecord, MyCursor> store = life.add(new StandardStore<>( new MyFormatWithHeader(14), new File("/store"),
                new TestStoreIdGenerator(), pageCache, fsRule.get() ));

        long recordId = store.allocate();

        store.write( new MyRecord( recordId, 1338) );

        // When
        MyRecord secondRecord = store.read( recordId );

        // Then
        assertThat(secondRecord.value, equalTo(1338l));

        // And when I restart the store
        life.shutdown();

        store = new StandardStore<>( new MyFormatWithHeader(14), new File("/store"),
                new TestStoreIdGenerator(), pageCache, fsRule.get() );
        store.init();
        store.start();

        assertThat( StoreMatchers.records( store ), equalTo( asList( new MyRecord( recordId, 1338 ) ) ) );
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

class MyCursor implements Store.RecordCursor<MyRecord>
{
    private final PagedFile file;
    private final StoreToolkit toolkit;
    private final MyHeaderLessFormat format;

    private PageCursor pageCursor;
    private long currentRecordId = -1;

    MyCursor( PagedFile file, StoreToolkit toolkit, MyHeaderLessFormat format )
    {
        this.file = file;
        this.toolkit = toolkit;
        this.format = format;
        this.currentRecordId = toolkit.firstRecordId() - 1;
    }

    @Override
    public MyRecord currentRecord()
    {
        MyRecord record = format.deserialize( currentRecordId, pageCursor );
        pageCursor.setOffset( toolkit.recordOffset( currentRecordId ) );
        return record;
    }

    public boolean inUse()
    {
        boolean inUse = format.inUse( pageCursor );
        pageCursor.setOffset( toolkit.recordOffset( currentRecordId ) );
        return inUse;
    }

    @Override
    public boolean next( long id )
    {
        long pageId = toolkit.pageId( id );
        int recordOffset = toolkit.recordOffset( id );

        if( pageCursor == null)
        {
            return moveToFirstPage( id, pageId );
        }
        else if( pageId == pageCursor.getCurrentPageId())
        {
            // The next record is in the same page, just reposition the cursor
            currentRecordId = id;
            pageCursor.setOffset( recordOffset );
            return true;
        }
        else
        {
            return moveToNextPage( id, pageId, recordOffset );
        }
    }

    private boolean moveToFirstPage( long id, long pageId )
    {
        try
        {
            pageCursor = file.io( pageId, PF_SHARED_LOCK );
            return next(id);
        }
        catch ( IOException e )
        {
            return false;
        }
    }

    private boolean moveToNextPage( long id, long pageId, int recordOffset )
    {
        try
        {
            if( pageCursor.next( pageId ))
            {
                currentRecordId = id;
                pageCursor.setOffset( recordOffset );
                return true;
            }
            else
            {
                return false;
            }
        }
        catch ( IOException e )
        {
            return false;
        }
    }

    @Override
    public boolean next()
    {
        while(next(currentRecordId+1))
        {
            if(inUse())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close()
    {
        pageCursor.close();
    }
}

class MyHeaderLessFormat extends FixedSizeRecordStoreFormat<MyRecord, MyCursor>
{
    public MyHeaderLessFormat()
    {
        super( 8, "MyRecord" );
    }

    @Override
    public MyCursor createCursor( PagedFile file, StoreToolkit toolkit )
    {
        return new MyCursor( file, toolkit, this );
    }

    @Override
    public long id( MyRecord myRecord )
    {
        return myRecord.id;
    }

    @Override
    public void serialize( PageCursor cursor, MyRecord myRecord )
    {
        cursor.putLong( myRecord.value );
    }

    @Override
    public MyRecord deserialize( long id, PageCursor cursor )
    {
        return new MyRecord( id, cursor.getLong() );
    }

    public boolean inUse( PageCursor pageCursor )
    {
        return pageCursor.getLong() != 0;
    }
}

class MyFormatWithHeader implements StoreFormat<MyRecord, MyCursor>
{
    private final int configuredRecordSize;

    MyFormatWithHeader( int configuredRecordSize )
    {
        this.configuredRecordSize = configuredRecordSize;
    }

    @Override
    public MyCursor createCursor( PagedFile file, StoreToolkit toolkit )
    {
        return new MyCursor( file, toolkit, new MyHeaderLessFormat() );
    }

    @Override
    public long id( MyRecord myRecord )
    {
        return myRecord.id;
    }

    @Override
    public void serialize( PageCursor cursor, MyRecord myRecord )
    {
        cursor.putLong( myRecord.value );
    }

    @Override
    public MyRecord deserialize( long id, PageCursor cursor )
    {
        return new MyRecord( id, cursor.getLong() );
    }

    @Override
    public String recordName()
    {
        return "MyHeaderedRecord";
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
}

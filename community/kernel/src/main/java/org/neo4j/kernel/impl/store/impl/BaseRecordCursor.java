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

import java.io.IOException;

import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.kernel.impl.store.Store;

import static org.neo4j.io.pagecache.PagedFile.PF_SHARED_LOCK;
import static org.neo4j.kernel.impl.store.impl.StoreFormat.RecordFormat;

public class BaseRecordCursor<RECORD> implements Store.RecordCursor<RECORD>
{
    private final PagedFile file;
    private final StoreToolkit toolkit;
    private final RecordFormat<RECORD> format;

    private PageCursor pageCursor;
    private long currentRecordId = -1;
    private int  currentRecordOffset = -1;

    public BaseRecordCursor( PagedFile file, StoreToolkit toolkit, RecordFormat<RECORD> format )
    {
        this.file = file;
        this.toolkit = toolkit;
        this.format = format;
        this.currentRecordId = toolkit.firstRecordId() - 1;
    }

    @Override
    public RECORD currentRecord()
    {
        RECORD record = format.deserialize( pageCursor, currentRecordOffset, currentRecordId );
        pageCursor.setOffset( toolkit.recordOffset( currentRecordId ) );
        return record;
    }

    public boolean inUse()
    {
        boolean inUse = format.inUse( pageCursor, currentRecordOffset );
        pageCursor.setOffset( toolkit.recordOffset( currentRecordId ) );
        return inUse;
    }

    @Override
    public boolean next( long id )
    {
        long pageId = toolkit.pageId( id );
        currentRecordOffset = toolkit.recordOffset( id );

        if( pageCursor == null)
        {
            return moveToFirstPage( id, pageId );
        }
        else if( pageId == pageCursor.getCurrentPageId())
        {
            // The next record is in the same page, just reposition the cursor
            currentRecordId = id;
            pageCursor.setOffset( currentRecordOffset );
            return true;
        }
        else
        {
            return moveToNextPage( id, pageId, currentRecordOffset );
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

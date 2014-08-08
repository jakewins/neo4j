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
package org.neo4j.kernel.impl.store.standard;

import java.io.IOException;

import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.kernel.impl.nioneo.store.UnderlyingStorageException;
import org.neo4j.kernel.impl.store.Store;

import static org.neo4j.io.pagecache.PagedFile.PF_SHARED_LOCK;
import static org.neo4j.kernel.impl.store.standard.StoreFormat.RecordFormat;

public class BaseRecordCursor<RECORD, FORMAT extends RecordFormat<RECORD>> implements Store.RecordCursor<RECORD>
{
    private final PagedFile file;

    // We share most of our fields with subclasses, as this is meant as an easy-to-use base for building custom
    // record cursors.

    protected final StoreToolkit toolkit;
    protected final FORMAT format;

    protected PageCursor pageCursor;
    protected long currentRecordId = -1;
    protected int  currentRecordOffset = -1;

    public BaseRecordCursor( PagedFile file, StoreToolkit toolkit, FORMAT format )
    {
        this.file = file;
        this.toolkit = toolkit;
        this.format = format;
        this.currentRecordId = toolkit.firstRecordId() - 1;
    }

    @Override
    public RECORD currentRecord()
    {
        return format.deserialize( pageCursor, currentRecordOffset, currentRecordId );
    }

    public boolean inUse()
    {
        return format.inUse( pageCursor, currentRecordOffset );
    }

    @Override
    public boolean next( long id )
    {
        try
        {
            long pageId = toolkit.pageId( id );
            currentRecordId = id;
            currentRecordOffset = toolkit.recordOffset( id );

            // This may be the first next() call, so we may need to initialize a PageCursor
            if ( pageCursor == null )
            {
                pageCursor = file.io( pageId, PF_SHARED_LOCK );
                return pageCursor.next( pageId );
            }
            // Either we're on the right page, or we move to it
            else return pageId == pageCursor.getCurrentPageId() || pageCursor.next( pageId );
        }
        catch(IOException e)
        {
            throw new UnderlyingStorageException( "Failed to read record " + id + ".", e );
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

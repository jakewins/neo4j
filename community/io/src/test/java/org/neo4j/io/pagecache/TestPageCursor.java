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
package org.neo4j.io.pagecache;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.neo4j.io.pagecache.impl.common.ByteBufferPage;
import org.neo4j.io.pagecache.impl.common.OffsetTrackingCursor;
import org.neo4j.io.pagecache.impl.common.Page;

/**
 * Utility for testing code that depends on page cursors.
 */
public class TestPageCursor extends OffsetTrackingCursor
{
    private long pageId;
    private int retriesRequired = 0;
    private int pageSize;

    // Used when retries are emulated to stash the page with the real data, to keep the user from reading it
    private Page realPage;

    public TestPageCursor(long initialPageId, int pageSize)
    {
        this.pageId = initialPageId;
        this.pageSize = pageSize;
        this.page = new ByteBufferPage( ByteBuffer.allocate(pageSize) );
    }

    @Override
    public long getCurrentPageId()
    {
        return pageId;
    }

    @Override
    public void rewind() throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean next() throws IOException
    {
        return false;
    }

    @Override
    public boolean next( long pageId ) throws IOException
    {
        return false;
    }

    @Override
    public void close()
    {

    }

    @Override
    public boolean retry()
    {
        if(retriesRequired > 0)
        {
            if(retriesRequired == 1)
            {
                page = realPage;
                realPage = null;
            }
            retriesRequired--;
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Force a number of retries
     */
    public void requireRetries( int numRetries )
    {
        retriesRequired = numRetries;
        if(retriesRequired > 0)
        {
            // Cheat, stash the real page away until retries have been fulfilled, to force user to read the wrong data
            realPage = page;
            page = new ByteBufferPage( ByteBuffer.allocate( pageSize ) );
        }
    }
}

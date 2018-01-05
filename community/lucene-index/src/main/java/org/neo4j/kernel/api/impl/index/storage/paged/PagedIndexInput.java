/*
 * Copyright (c) 2002-2018 "Neo Technology,"
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
package org.neo4j.kernel.api.impl.index.storage.paged;

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.RandomAccessInput;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.neo4j.function.ThrowingAction;
import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;

public class PagedIndexInput extends IndexInput implements RandomAccessInput
{
    private final long inputSize;
    private final int pageSize;

    // Used when this input is a slice of the full input, pointing to the logical
    // start point of the input
    private final long startPosition;

    private final long endPageId;
    private final int endPageOffset;

    private final ThrowingAction<IOException> onClose;
    private PagedFile pagedfile;
    private PageCursor cursor;

    private long currentPageId;
    private int currentPageOffset;

    public static PagedIndexInput newInstance( String resourceDescription, PagedFile pagedFile, long startPosition, long size,
            ThrowingAction<IOException> onClose ) throws IOException
    {
        return new PagedIndexInput( resourceDescription, pagedFile, startPosition, size, onClose );
    }

    PagedIndexInput( String resourceDescription, PagedFile pagedFile, long startPosition, long size, ThrowingAction<IOException> onClose ) throws IOException
    {
        super( resourceDescription );
        this.onClose = onClose;
        this.pagedfile = pagedFile;
        this.pageSize = pagedFile.pageSize();
        this.inputSize = size;
        this.startPosition = startPosition;
        this.currentPageId = pageId( 0 );
        this.currentPageOffset = pageOffset( 0 );
        this.endPageId = pageId( startPosition + size );
        this.endPageOffset = calcLastPageOffset( startPosition + size, pageSize );
        this.cursor = pagedFile.io( currentPageId, PagedFile.PF_SHARED_READ_LOCK );
    }

    private static int calcLastPageOffset( long fileSize, int pageSize )
    {
        if ( fileSize == 0 )
        {
            return 0;
        }
        int size = (int) (fileSize % pageSize);
        return size == 0 ? pageSize : size;
    }

    private long pageId( long position )
    {
        return (startPosition + position) / pageSize;
    }

    private int pageOffset( long position )
    {
        return (int) ((startPosition + position) % pageSize);
    }

    @Override
    public final byte readByte() throws IOException
    {
        moveCursorToPageAndAssertInBounds( currentPageId, currentPageOffset );

        byte val;
        do
        {
            val = cursor.getByte( currentPageOffset );
        }
        while ( cursor.shouldRetry() );

        boundsCheck();
        incrementPageOffset( 1 );

        return val;
    }

    @Override
    public byte readByte( long pos ) throws IOException
    {
        long pageId = pageId( pos );
        int pageOffset = pageOffset( pos );

        moveCursorToPageAndAssertInBounds( pageId, pageOffset );

        byte val;
        do
        {
            val = cursor.getByte( pageOffset );
        }
        while ( cursor.shouldRetry() );

        boundsCheck();

        return val;
    }

    @Override
    public short readShort() throws IOException
    {
        short val = readShort( currentPageId, currentPageOffset );
        incrementPageOffset( 2 );
        return val;
    }

    @Override
    public short readShort( long pos ) throws IOException
    {
        return readShort( pageId( pos ), pageOffset( pos ) );
    }

    private short readShort( long pageId, int pageOffset ) throws IOException
    {
        // Cross-page read?
        if ( pageSize - pageOffset < 2 )
        {
            long originalPageId = currentPageId;
            int originalPageOffset = currentPageOffset;
            try
            {
                currentPageId = pageId;
                currentPageOffset = pageOffset;
                // Delegate to parent, which reads byte-by-byte
                return super.readShort();
            }
            finally
            {
                currentPageId = originalPageId;
                currentPageOffset = originalPageOffset;
            }
        }

        // Regular read
        moveCursorToPageAndAssertInBounds( pageId, pageOffset );

        short val;
        do
        {
            val = cursor.getShort( pageOffset );
        }
        while ( cursor.shouldRetry() );

        boundsCheck();

        return val;
    }

    @Override
    public int readInt() throws IOException
    {
        int val = readInt( currentPageId, currentPageOffset );
        incrementPageOffset( 4 );
        return val;
    }

    @Override
    public int readInt( long pos ) throws IOException
    {
        return readInt( pageId( pos ), pageOffset( pos ) );
    }

    private int readInt( long pageId, int pageOffset ) throws IOException
    {
        // Cross-page read?
        if ( pageSize - pageOffset < 4 )
        {
            long originalPageId = currentPageId;
            int originalPageOffset = currentPageOffset;
            try
            {
                currentPageId = pageId;
                currentPageOffset = pageOffset;
                // Delegate to parent, which reads byte-by-byte
                return super.readInt();
            }
            finally
            {
                currentPageId = originalPageId;
                currentPageOffset = originalPageOffset;
            }
        }

        // Regular read
        moveCursorToPageAndAssertInBounds( pageId, pageOffset );

        int val;
        do
        {
            val = cursor.getInt( pageOffset );
        }
        while ( cursor.shouldRetry() );

        boundsCheck();

        return val;
    }

    @Override
    public long readLong() throws IOException
    {
        long val = readLong( currentPageId, currentPageOffset );
        incrementPageOffset( 8 );
        return val;
    }

    @Override
    public long readLong( long pos ) throws IOException
    {
        return readLong( pageId( pos ), pageOffset( pos ) );
    }

    private long readLong( long pageId, int pageOffset ) throws IOException
    {
        // Cross-page read?
        if ( pageSize - pageOffset < 8 )
        {
            long originalPageId = currentPageId;
            int originalPageOffset = currentPageOffset;
            try
            {
                currentPageId = pageId;
                currentPageOffset = pageOffset;
                // Delegate to parent, which reads byte-by-byte
                return super.readLong();
            }
            finally
            {
                currentPageId = originalPageId;
                currentPageOffset = originalPageOffset;
            }
        }

        // Regular read
        moveCursorToPageAndAssertInBounds( pageId, pageOffset );

        long val;
        do
        {
            val = cursor.getLong( pageOffset );
        }
        while ( cursor.shouldRetry() );

        boundsCheck();

        return val;
    }

    @Override
    public final void readBytes( final byte[] b, final int offset, final int len ) throws IOException
    {
        int bytesRead = 0;

        while ( bytesRead < len )
        {
            moveCursorToPageAndAssertInBounds( currentPageId, currentPageOffset );

            int toRead = Math.min( len - bytesRead, pageSize - currentPageOffset );
            do
            {
                cursor.setOffset( currentPageOffset );
                cursor.getBytes( b, offset + bytesRead, toRead );
            }
            while ( cursor.shouldRetry() );

            boundsCheck();
            incrementPageOffset( toRead );
            bytesRead += toRead;
        }
    }

    @Override
    public long getFilePointer()
    {
        try
        {
            return (currentPageId * cursor.getCurrentPageSize() + currentPageOffset) - startPosition;
        }
        catch ( NullPointerException npe )
        {
            throw new AlreadyClosedException( "Already closed: " + this );
        }
    }

    @Override
    public void seek( long pos ) throws IOException
    {
        long newPageId = pageId( pos );
        int newOffset = pageOffset( pos );

        if ( isEOF( newPageId, newOffset ) )
        {
            throw new EOFException( "seek past EOF: " + this );
        }

        currentPageId = newPageId;
        currentPageOffset = newOffset;
    }

    private void boundsCheck() throws EOFException
    {
        if ( cursor.checkAndClearBoundsFlag() )
        {
            throw new EOFException( "read past EOF: " + this );
        }
    }

    private void incrementPageOffset( int byOffset )
    {
        currentPageOffset += byOffset;
        if ( currentPageOffset >= pageSize )
        {
            currentPageId += 1;
            currentPageOffset -= pageSize;
            assert currentPageOffset <= pageSize : "Should never read past two page boundaries";
        }
    }

    private void moveCursorToPageAndAssertInBounds( long pageId, int offsetInPage ) throws IOException
    {
        if ( !cursor.next( pageId ) || isEOF( pageId, offsetInPage ) )
        {
            throw new EOFException( "read past EOF: " + this );
        }
    }

    private boolean isEOF( long pageId, int offsetInPage )
    {
        boolean isBeforeLastPage = pageId > endPageId;
        boolean isLastPage = pageId == endPageId;
        boolean offsetOutsidePage = offsetInPage >= endPageOffset;
        return isBeforeLastPage || (isLastPage && offsetOutsidePage);
    }

    @Override
    public final long length()
    {
        return inputSize;
    }

    @Override
    public final PagedIndexInput clone()
    {
        return slice( "clone", 0, length() );
    }

    /**
     * Creates a slice of this index input, with the given description, offset, and length. The slice is seeked to the beginning.
     */
    @Override
    public final PagedIndexInput slice( String sliceDescription, long offset, long length )
    {
        // Implementation notes:
        // This is used for both slice() and clone()
        // Lucene does not close these derivative inputs; it only closes the original one.
        // Hence, all resource cleanup needs to happen in the original PagedIndexInput.
        if ( offset < 0 || length < 0 || offset + length > this.inputSize )
        {
            throw new IllegalArgumentException(
                    "slice() " + sliceDescription + " out of bounds: offset=" + offset + ",length=" + length + ",fileLength=" + this.inputSize + ": " + this );
        }

        try
        {
            return newInstance( sliceDescription, pagedfile, startPosition + offset, length, null );
        }
        catch ( IOException e )
        {
            // Can't throw checked IOException due to the Lucene API
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public final void close() throws IOException
    {
        // TODO: This isn't good enough.
        // Lucene extensively uses #clone() and #slice(), and does not close those inputs;
        // it only closes the original parent input. Hence, all children we create need to be tracked
        // so we can close their associated cursors when the parent is closed. Lucene does this with
        // a WeakIdentityHashmap, see ByteBufferIndexInput.
        try
        {
            cursor.close();
        }
        finally
        {
            cursor = null;
        }
        if ( onClose != null )
        {
            onClose.apply();
        }
    }
}

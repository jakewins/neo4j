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

import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;

public class PagedIndexInput extends IndexInput implements RandomAccessInput
{
    /** Tracks open resources across cloned inputs, see {@link InputResources}. */
    final InputResources resources;

    /** Star of the show */
    PageCursor cursor;

    /**
     * The logical size of this input. Same as file size if this is a root input,
     * but can be anything <= file size if this is a clone
     */
    private final long inputSize;

    /** Fast access to pagedFile.pageSize() */
    private final int pageSize;

    /**
     * Logical start position in the file, 0 for root inputs, anything <= file size for
     * clones and slices
     */
    private final long startPosition;

    /** Last page this input can access - last actual page for roots, any page for clones */
    private final long endPageId;

    /** Final offset in last page this input can access - last actual offset for root, any offset for clones */
    private final int endPageOffset;

    private long currentPageId;
    private int currentPageOffset;

    PagedIndexInput( String resourceDescription, PagedFile pagedFile, long startPosition, long size ) throws IOException
    {
        this( new InputResources.RootInputResources( pagedFile ), resourceDescription, startPosition, pagedFile.pageSize(), size );
    }

    PagedIndexInput( InputResources resources, String resourceDescription, long startPosition, int pageSize, long size ) throws IOException
    {
        super( resourceDescription );

        // Note: This constructor does not set cloneRoot, meaning it can only be used by other constructors that
        // then set cloneRoot, otherwise the resulting object will be broken.
        this.pageSize = pageSize;
        this.inputSize = size;
        this.startPosition = startPosition;
        this.currentPageId = pageId( 0 );
        this.currentPageOffset = pageOffset( 0 );
        this.endPageId = pageId( startPosition + size );
        this.endPageOffset = calcLastPageOffset( startPosition + size, pageSize );

        this.resources = resources;
        this.cursor = resources.openCursor( currentPageId, this );
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

    @Override
    public long getFilePointer()
    {
        try
        {
            return (currentPageId * pageSize + currentPageOffset) - startPosition;
        }
        catch ( NullPointerException npe )
        {
            throw new AlreadyClosedException( "Already closed: " + this );
        }
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
        // Lucene does not close these child inputs; it only closes the root one.
        // See PagedIndexInputCloningTest for details.
        if ( offset < 0 || length < 0 || offset + length > this.inputSize )
        {
            throw new IllegalArgumentException(
                    "slice() " + sliceDescription + " out of bounds: offset=" + offset + ",length=" + length + ",fileLength=" + this.inputSize + ": " + this );
        }

        try
        {
            return new PagedIndexInput( resources.cloneResources(), sliceDescription, startPosition + offset, pageSize, length );
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
        resources.close( this );
    }
}

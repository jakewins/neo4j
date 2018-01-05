package org.neo4j.kernel.api.impl.index.storage.paged;

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.RandomAccessInput;
import org.apache.lucene.util.WeakIdentityMap;

import java.io.EOFException;
import java.io.IOException;

import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.io.pagecache.PagedFile;

public class PagedIndexInput extends IndexInput implements RandomAccessInput
{
    private final long length;
    private final int pageSize;
    private final long lastPageId;

    private PagedFile file;
    private PageCursor cursor;

    private long currentPageId = 0;
    private int currentPageOffset = 0;

    private final WeakIdentityMap<PagedIndexInput,Boolean> clones;

    public static PagedIndexInput newInstance( String resourceDescription, PagedFile file, boolean trackClones ) throws IOException
    {
        final WeakIdentityMap<PagedIndexInput,Boolean> clones = trackClones ? WeakIdentityMap.newConcurrentHashMap() : null;
        return new PagedIndexInput( resourceDescription, file, clones );
    }

    PagedIndexInput( String resourceDescription, PagedFile file, WeakIdentityMap<PagedIndexInput,Boolean> clones ) throws IOException
    {
        super( resourceDescription );
        this.file = file;
        this.cursor = file.io( 0, PagedFile.PF_SHARED_READ_LOCK );
        this.length = file.fileSize();
        this.pageSize = file.pageSize();
        this.lastPageId = file.getLastPageId();
        this.clones = clones;
    }

    private long pageId( long position )
    {
        return position / pageSize;
    }

    private int pageOffset( long position )
    {
        return (int) (position % pageSize);
    }

    @Override
    public final byte readByte() throws IOException
    {
        if ( !cursor.next( currentPageId ) )
        {
            throw new EOFException( "read past EOF: " + this );
        }

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
        if ( !cursor.next( pageId ) )
        {
            throw new EOFException( "read past EOF: " + this );
        }

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
        if ( !cursor.next( pageId ) )
        {
            throw new EOFException( "read past EOF: " + this );
        }

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
        if ( !cursor.next( pageId ) )
        {
            throw new EOFException( "read past EOF: " + this );
        }

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
        if ( !cursor.next( pageId ) )
        {
            throw new EOFException( "read past EOF: " + this );
        }

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
            if ( !cursor.next( currentPageId ) )
            {
                throw new EOFException( "read past EOF: " + this );
            }

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

    @Override
    public long getFilePointer()
    {
        try
        {
            return currentPageId * cursor.getCurrentPageSize() + currentPageOffset;
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

        if ( newPageId > lastPageId )
        {
            throw new EOFException( "seek past EOF: " + this );
        }

        currentPageId = newPageId;
        currentPageOffset = newOffset;
    }

    @Override
    public final long length()
    {
        return length;
    }

    @Override
    public final PagedIndexInput clone()
    {
        // See BufferedIndexInput
        throw new UnsupportedOperationException( "Not implemented" );
    }

    /**
     * Creates a slice of this index input, with the given description, offset, and length. The slice is seeked to the beginning.
     */
    @Override
    public final PagedIndexInput slice( String sliceDescription, long offset, long length )
    {
        if ( offset < 0 || length < 0 || offset + length > this.length )
        {
            throw new IllegalArgumentException(
                    "slice() " + sliceDescription + " out of bounds: offset=" + offset + ",length=" + length + ",fileLength=" + this.length + ": " + this );
        }

        // See BufferedIndexInput
        throw new UnsupportedOperationException( "Not implemented" );
    }

    @Override
    public final void close() throws IOException
    {
        try
        {
            file.close();
        }
        finally
        {
            file = null;
        }
    }
}
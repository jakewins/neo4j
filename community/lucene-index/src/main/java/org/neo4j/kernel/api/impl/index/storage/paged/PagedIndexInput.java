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

    public static PagedIndexInput newInstance( String resourceDescription, PagedFile file, boolean trackClones )
            throws IOException
    {
        final WeakIdentityMap<PagedIndexInput,Boolean> clones = trackClones ? WeakIdentityMap.newConcurrentHashMap() : null;
        return new PagedIndexInput(resourceDescription, file, clones);
    }

    PagedIndexInput( String resourceDescription, PagedFile file, WeakIdentityMap<PagedIndexInput,Boolean> clones )
            throws IOException
    {
        super(resourceDescription);
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

    private int offset( long position )
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
    public byte readByte(long pos) throws IOException {
        // TODO
        throw new UnsupportedOperationException( "TODO" );
//        try {
//            final int bi = (int) (pos >> chunkSizePower);
//            return buffers[bi].get((int) (pos & chunkSizeMask));
//        } catch (IndexOutOfBoundsException ioobe) {
//            throw new EOFException("seek past EOF: " + this);
//        } catch (NullPointerException npe) {
//            throw new AlreadyClosedException("Already closed: " + this);
//        }
    }

    // TODO: Implemented optimized readLong, readInt, readShort

    @Override
    public final void readBytes(final byte[] b, final int offset, final int len) throws IOException {
        int bytesRead = 0;

        while(bytesRead < len)
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
        if( currentPageOffset >= pageSize )
        {
            assert currentPageOffset == pageSize : "Should never read past page boundary.";
            currentPageId += 1;
            currentPageOffset = 0;
        }
    }

    @Override
    public long getFilePointer() {
        // TODO test!
        try {
            return currentPageId * cursor.getCurrentPageSize() + currentPageOffset;
        } catch (NullPointerException npe) {
            throw new AlreadyClosedException("Already closed: " + this);
        }
    }

    @Override
    public void seek(long pos) throws IOException {
        long newPageId = pageId( pos );
        int newOffset = offset( pos );

        if(newPageId > lastPageId) {
            throw new EOFException("seek past EOF: " + this);
        }

        currentPageId = newPageId;
        currentPageOffset = newOffset;
    }

    // used only by random access methods to handle reads across boundaries
    private void setPos(long pos, int bi) throws IOException {
//        try {
//            final ByteBuffer b = buffers[bi];
//            b.position((int) (pos & chunkSizeMask));
//            this.curBufIndex = bi;
//            this.curBuf = b;
//        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException aioobe) {
//            throw new EOFException("seek past EOF: " + this);
//        } catch (NullPointerException npe) {
//            throw new AlreadyClosedException("Already closed: " + this);
//        }
    }

    @Override
    public short readShort(long pos) throws IOException {
        // TODO!
        throw new UnsupportedOperationException( ".." );
    }

    @Override
    public int readInt(long pos) throws IOException {
        // TODO!
        throw new UnsupportedOperationException( ".." );
    }

    @Override
    public long readLong(long pos) throws IOException {
        // TODO!
        throw new UnsupportedOperationException( ".." );
//        final int bi = (int) (pos >> chunkSizePower);
//        try {
//            return buffers[bi].getLong((int) (pos & chunkSizeMask));
//        } catch (IndexOutOfBoundsException ioobe) {
//            // either it's a boundary, or read past EOF, fall back:
//            setPos(pos, bi);
//            return readLong();
//        } catch (NullPointerException npe) {
//            throw new AlreadyClosedException("Already closed: " + this);
//        }
    }

    @Override
    public final long length() {
        return length;
    }

    @Override
    public final PagedIndexInput clone() {
        // See BufferedIndexInput
        throw new UnsupportedOperationException( "Not implemented" );
    }

    /**
     * Creates a slice of this index input, with the given description, offset, and length. The slice is seeked to the beginning.
     */
    @Override
    public final PagedIndexInput slice(String sliceDescription, long offset, long length) {
        if (offset < 0 || length < 0 || offset+length > this.length) {
            throw new IllegalArgumentException("slice() " + sliceDescription + " out of bounds: offset=" + offset + ",length=" + length + ",fileLength="  + this.length + ": "  + this);
        }

        // See BufferedIndexInput
        throw new UnsupportedOperationException( "Not implemented" );
    }

    @Override
    public final void close() throws IOException {
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
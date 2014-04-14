package org.neo4j.batch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import org.neo4j.helpers.UTF8;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.nioneo.store.IdGeneratorImpl;
import org.neo4j.kernel.impl.nioneo.store.Record;
import org.neo4j.kernel.impl.storemigration.legacystore.LegacyStore;

public class RelGroupStoreReader implements Closeable
{
    public static class ReusableRelGroup
    {
        private long recordId;
        private boolean inUse;
        private int type;
        private long next;
        private long firstIn;
        private long firstOut;
        private long firstLoop;

        public ReusableRelGroup reset( long recordId, boolean inUse, int type, long next, long firstIn, long firstOut, long
                firstLoop )
        {
            this.recordId = recordId;
            this.inUse = inUse;
            this.type = type;
            this.next = next;
            this.firstIn = firstIn;
            this.firstOut = firstOut;
            this.firstLoop = firstLoop;
            return this;
        }

        public boolean inUse()
        {
            return inUse;
        }

        public long id()
        {
            return recordId;
        }

        public int getType()
        {
            return type;
        }
    }

    private static class BufferAndPosition
    {
        private long position;
        private ByteBuffer buffer;

        public BufferAndPosition( long position, ByteBuffer buffer )
        {
            this.position = position;
            this.buffer = buffer;
        }

        public BufferAndPosition reset( long position )
        {
            buffer.clear();
            this.position = position;
            return this;
        }
    }

    private CompletionHandler<Integer, BufferAndPosition> READ_COMPLETED = new CompletionHandler<Integer, BufferAndPosition>()

    {
        @Override
        public void completed( Integer result, BufferAndPosition attachment )
        {
            try
            {
                reads.put( attachment );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        }

        @Override
        public void failed( Throwable exc, BufferAndPosition attachment )
        {
            exc.printStackTrace();
        }
    };

    public static final String FROM_VERSION = "RelationshipGroupStore " + LegacyStore.LEGACY_VERSION;
    public static final int RECORD_SIZE = 20;
    private static final int BUFFER_SIZE =  4 * 1024 * RECORD_SIZE;

    private final long maxId;
    private final AsynchronousFileChannel channel;

    private final ArrayBlockingQueue<BufferAndPosition> reads = new ArrayBlockingQueue<>( 8 );

    public RelGroupStoreReader( File fileName ) throws IOException
    {
        channel = IO.openAsync( fileName.getAbsolutePath() );
        int endHeaderSize = UTF8.encode( FROM_VERSION ).length;
        maxId = (channel.size() - endHeaderSize) / RECORD_SIZE;
    }

    public long getMaxId()
    {
        return maxId;
    }

    /**
     * @param approximateStartId the scan will start at the beginning of the page this id is located in.
     */
    public void accept( long approximateStartId, Visitor<ReusableRelGroup, RuntimeException> visitor ) throws IOException
    {
        ReusableRelGroup rel = new ReusableRelGroup();

        long position = (approximateStartId * RECORD_SIZE) - ( (approximateStartId * RECORD_SIZE) % BUFFER_SIZE),
                fileSize = channel.size();

        // Fire off initial reads
        for ( int i = 0; i < 8; i++ )
        {
            ByteBuffer buffer = ByteBuffer.allocateDirect( BUFFER_SIZE );
            channel.read( buffer, position, new BufferAndPosition(position, buffer), READ_COMPLETED );
            position += BUFFER_SIZE;
        }

        // Start processing
        ArrayList<BufferAndPosition> batch = new ArrayList<>( 8 );
        while(position < fileSize)
        {
            int recordOffset = 0;

            batch.clear();
            reads.drainTo( batch, 8 );

            for ( BufferAndPosition data : batch )
            {
                ByteBuffer buffer = data.buffer;
                while(recordOffset < buffer.capacity() && (recordOffset + position) < fileSize)
                {
                    buffer.position(recordOffset);
                    long id = (data.position + recordOffset) / RECORD_SIZE;

                    readRecord(buffer, id, rel);

                    if(visitor.visit( rel ))
                    {
                        return;
                    }

                    recordOffset += RECORD_SIZE;
                }

                position += BUFFER_SIZE;
                if(position < fileSize)
                {
                    channel.read( data.buffer, position, data.reset(position), READ_COMPLETED );
                }
            }
        }

        // TODO: Wait for all pending reads to complete here.
    }

    private void readRecord( ByteBuffer buffer, long id, ReusableRelGroup rel)
    {
        long inUseByte = buffer.get();

        boolean inUse = (inUseByte & 0x1) == Record.IN_USE.intValue();
        if ( inUse )
        {
            // [    ,xxx ] high firstIn bits
            // [ xxx,    ] high firstLoop bits
            long highByte = buffer.get();

            int type = buffer.getShort();
            long nextLowBits = getUnsignedInt( buffer );
            long nextOutLowBits = getUnsignedInt( buffer );
            long nextInLowBits = getUnsignedInt( buffer );
            long nextLoopLowBits = getUnsignedInt( buffer );

            long nextMod = (inUseByte & 0xE) << 31;
            long nextOutMod = (inUseByte & 0x70) << 28;
            long nextInMod = (highByte & 0xE) << 31;
            long nextLoopMod = (highByte & 0x70) << 28;

            long next = longFromIntAndMod( nextLowBits, nextMod );
            long firstOut = longFromIntAndMod( nextOutLowBits, nextOutMod );
            long firstIn = longFromIntAndMod( nextInLowBits, nextInMod );
            long firstLoop = longFromIntAndMod( nextLoopLowBits, nextLoopMod );

            rel.reset( id, true, type, next, firstIn, firstOut, firstLoop );
        }
        else
        {
            rel.reset( id, false, -1, -1, -1, -1, -1 );
        }
    }
    public static long getUnsignedInt( ByteBuffer buf )
    {
        return buf.getInt()&0xFFFFFFFFL;
    }

    protected static long longFromIntAndMod( long base, long modifier )
    {
        return modifier == 0 && base == IdGeneratorImpl.INTEGER_MINUS_ONE ? -1 : base|modifier;
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
    }
}

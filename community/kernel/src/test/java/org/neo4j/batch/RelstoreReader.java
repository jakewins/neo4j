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
import org.neo4j.kernel.impl.nioneo.store.RelationshipRecord;
import org.neo4j.kernel.impl.storemigration.legacystore.LegacyStore;

public class RelstoreReader implements Closeable
{
    public static class ReusableRelationship
    {
        private long recordId;
        private boolean inUse;
        private long firstNode;
        private long secondNode;
        private int type;
        private long firstPrevRel;
        private long firstNextRel;
        private long secondNextRel;
        private long secondPrevRel;
        private long nextProp;

        private RelationshipRecord record;
        private boolean firstInFirstChain;
        private boolean firstInSecondChain;

        public void reset( long id, boolean inUse, long firstNode, long secondNode, int type, long firstPrevRel,
                           long firstNextRel, long secondNextRel, long secondPrevRel, long nextProp, boolean
                firstInFirstChain, boolean firstInSecondChain )
        {
            this.firstInFirstChain = firstInFirstChain;
            this.firstInSecondChain = firstInSecondChain;
            this.record = null;
            this.recordId = id;
            this.inUse = inUse;
            this.firstNode = firstNode;
            this.secondNode = secondNode;
            this.type = type;
            this.firstPrevRel = firstPrevRel;
            this.firstNextRel = firstNextRel;
            this.secondNextRel = secondNextRel;
            this.secondPrevRel = secondPrevRel;
            this.nextProp = nextProp;
        }

        public boolean inUse()
        {
            return inUse;
        }

        public long getFirstNode()
        {
            return firstNode;
        }

        public long getFirstNextRel()
        {
            return firstNextRel;
        }

        public long getSecondNode()
        {
            return secondNode;
        }

        public long getFirstPrevRel()
        {
            return firstPrevRel;
        }

        public long getSecondPrevRel()
        {
            return secondPrevRel;
        }

        public long getSecondNextRel()
        {
            return secondNextRel;
        }

        public long id()
        {
            return recordId;
        }

        public RelationshipRecord createRecord()
        {
            if( record == null)
            {
                record = new RelationshipRecord( recordId, firstNode, secondNode, type );
                record.setInUse( inUse );
                record.setFirstPrevRel( firstPrevRel );
                record.setFirstNextRel( firstNextRel );
                record.setSecondPrevRel( secondPrevRel );
                record.setSecondNextRel( secondNextRel );
                record.setNextProp( nextProp );
                record.setFirstInFirstChain( firstInFirstChain );
                record.setFirstInSecondChain( firstInSecondChain );

            }
            return record;
        }

        public int getType()
        {
            return type;
        }

        public boolean firstInFirstChain()
        {
            return firstInFirstChain;
        }

        public boolean firstInSecondChain()
        {
            return firstInSecondChain;
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

    public static final String FROM_VERSION = "RelationshipStore " + LegacyStore.LEGACY_VERSION;
    public static final int RECORD_SIZE = 34;
    private static final int BUFFER_SIZE =  4 * 1024 * RECORD_SIZE;

    private final long maxId;
    private final AsynchronousFileChannel channel;

    private final ArrayBlockingQueue<BufferAndPosition> reads = new ArrayBlockingQueue<>( 8 );

    public RelstoreReader( File fileName ) throws IOException
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
    public void accept( long approximateStartId, Visitor<ReusableRelationship, RuntimeException> visitor ) throws IOException
    {
        ReusableRelationship rel = new ReusableRelationship();

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

    private void readRecord( ByteBuffer buffer, long id, ReusableRelationship rel)
    {
        long inUseByte = buffer.get();

        boolean inUse = (inUseByte & 0x1) == Record.IN_USE.intValue();
        if ( inUse )
        {
            long firstNode = getUnsignedInt( buffer );
            long firstNodeMod = (inUseByte & 0xEL) << 31;

            long secondNode = getUnsignedInt( buffer );

            // [ xxx,    ][    ,    ][    ,    ][    ,    ] second node high order bits,     0x70000000
            // [    ,xxx ][    ,    ][    ,    ][    ,    ] first prev rel high order bits,  0xE000000
            // [    ,   x][xx  ,    ][    ,    ][    ,    ] first next rel high order bits,  0x1C00000
            // [    ,    ][  xx,x   ][    ,    ][    ,    ] second prev rel high order bits, 0x380000
            // [    ,    ][    , xxx][    ,    ][    ,    ] second next rel high order bits, 0x70000
            // [    ,    ][    ,    ][xxxx,xxxx][xxxx,xxxx] type
            long typeInt = buffer.getInt();
            long secondNodeMod = (typeInt & 0x70000000L) << 4;
            int type = (int) (typeInt & 0xFFFF);

            firstNode = longFromIntAndMod( firstNode, firstNodeMod );
            secondNode = longFromIntAndMod( secondNode, secondNodeMod );

            long firstPrevRel = getUnsignedInt( buffer );
            long firstPrevRelMod = (typeInt & 0xE000000L) << 7;
            firstPrevRel =  longFromIntAndMod( firstPrevRel, firstPrevRelMod );

            long firstNextRel = getUnsignedInt( buffer );
            long firstNextRelMod = (typeInt & 0x1C00000L) << 10;
            firstNextRel = longFromIntAndMod( firstNextRel, firstNextRelMod );

            long secondPrevRel = getUnsignedInt( buffer );
            long secondPrevRelMod = (typeInt & 0x380000L) << 13;
            secondPrevRel = longFromIntAndMod( secondPrevRel, secondPrevRelMod );

            long secondNextRel = getUnsignedInt( buffer );
            long secondNextRelMod = (typeInt & 0x70000L) << 16;
            secondNextRel = longFromIntAndMod( secondNextRel, secondNextRelMod );

            long nextProp = getUnsignedInt( buffer );
            long nextPropMod = (inUseByte & 0xF0L) << 28;
            nextProp = longFromIntAndMod( nextProp, nextPropMod );

            byte extraByte = buffer.get();

            boolean firstInFirstChain =  (extraByte & 0x1) != 0;
            boolean firstInSecondChain = ( (extraByte & 0x2) != 0 );

            rel.reset( id, true, firstNode, secondNode, type,
                    firstPrevRel, firstNextRel, secondNextRel, secondPrevRel, nextProp,firstInFirstChain, firstInSecondChain );
        }
        else
        {
            rel.reset( id, false, -1, -1, -1, -1, -1, -1, -1, -1, false, false );
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

package org.neo4j.batch;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class NodeProcessor extends Thread
{
    private final ArrayBlockingQueue<RelChain> incoming;
    private final ArrayBlockingQueue<RelChain> completedChains;
    private final AtomicReference<Throwable> writerException;

    public static RelChain POISON = new RelChain( -1 );
    private static final int BATCH_SIZE = 128;

    public NodeProcessor( ArrayBlockingQueue<RelChain> incoming, ArrayBlockingQueue<RelChain> completedChains, AtomicReference<Throwable> writerException )
    {
        this.incoming = incoming;
        this.completedChains = completedChains;
        this.writerException = writerException;
    }

    private final Collection<RelChain> batch = new AbstractCollection<RelChain>()
    {
        private final RelChain[] data = new RelChain[BATCH_SIZE];
        private int writeCursor = 0;
        private int readCursor = 0;

        private final Iterator<RelChain> iterator = new Iterator<RelChain>()
        {
            @Override
            public boolean hasNext()
            {
                return readCursor < writeCursor;
            }

            @Override
            public RelChain next()
            {
                return data[readCursor++];
            }
        };

        @Override
        public boolean add( RelChain relChain )
        {
            data[writeCursor++] = relChain;
            return false;
        }

        @Override
        public Iterator<RelChain> iterator()
        {
            readCursor = 0;
            return iterator;
        }

        @Override
        public int size()
        {
            throw new UnsupportedOperationException(  );
        }

        @Override
        public void clear()
        {
            readCursor = 0;
            writeCursor = 0;
        }
    };

    @Override
    public void run()
    {
        while(true)
        {
            batch.clear();
            incoming.drainTo( batch, BATCH_SIZE );

            for ( RelChain chain : batch )
            {
                if(chain == POISON)
                {
                    return;
                }

                chain.reset();
                completedChains.offer( chain ); // best effort
            }

        }
    }
}

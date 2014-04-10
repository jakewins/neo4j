package org.neo4j.batch;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class NodeProcessor extends Thread
{
    private final ArrayBlockingQueue<RelChain> incoming;
    private final AtomicReference<Throwable> writerException;

    public static RelChain POISON = new RelChain( -1 );

    public NodeProcessor( ArrayBlockingQueue<RelChain> incoming, AtomicReference<Throwable> writerException )
    {
        this.incoming = incoming;
        this.writerException = writerException;
    }

    @Override
    public void run()
    {
        ArrayList<RelChain> chains = new ArrayList<>(128);
        while(true)
        {
            chains.clear();
            incoming.drainTo( chains, 128 );

            for ( RelChain chain : chains )
            {
                if(chain == POISON)
                {
                    return;
                }
            }

        }
    }
}

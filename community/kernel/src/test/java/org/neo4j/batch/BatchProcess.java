package org.neo4j.batch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.storemigration.monitoring.VisibleMigrationProgressMonitor;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.impl.util.collection.LongObjectOpenHashMap;

import static org.neo4j.batch.RelstoreReader.ReusableRelationship;

public class BatchProcess
{

    public static final DynamicRelationshipType FOLLOWS = DynamicRelationshipType.withName( "FOLLOWS" );

    private int percentComplete;
    private long totalEntities;
    private VisibleMigrationProgressMonitor progressMonitor;

    public static void main(String ... args) throws IOException
    {
        long start = System.currentTimeMillis();
        new BatchProcess().run();
        long delta = System.currentTimeMillis() - start;
        System.out.println(delta / 1000.0);
    }

    private void run() throws IOException
    {
        final RelstoreReader reader = new RelstoreReader(
                new File("/Users/jake/Code/neo4j-root/big-db/neostore.relationshipstore.db") );

        totalEntities = reader.getMaxId();
        progressMonitor = new VisibleMigrationProgressMonitor( StringLogger.DEV_NULL, System.out );

        System.out.println("Processing " + totalEntities + " rels.");

        final ArrayBlockingQueue<RelChain> chainsToWrite = new ArrayBlockingQueue<>( 24 );
        final AtomicReference<Throwable> writerException = new AtomicReference<>();

        Thread writerThread = new NodeProcessor( chainsToWrite, writerException );
        writerThread.start();

        try
        {
            // Determined through testing to be a reasonable weigh-off between risk/benefit
            final int maxSimultaneousNodes = (int) (240 * (Runtime.getRuntime().totalMemory() / (1024 * 1024)));
            final AtomicBoolean morePassesRequired = new AtomicBoolean(false);
            final AtomicLong firstRelationshipRequiringANewPass = new AtomicLong(0l);
            final LongObjectOpenHashMap<RelChain> relChains = new LongObjectOpenHashMap<>( maxSimultaneousNodes );
            long numberOfPasses = 1;
            do
            {
                percentComplete = 0;
                if(morePassesRequired.get())
                {
                    System.out.println( " [MultiPass] Finished pass #" + (numberOfPasses-1) );
                    numberOfPasses++;
                }

                reader.accept( firstRelationshipRequiringANewPass.get(),
                        new Visitor<ReusableRelationship, RuntimeException>()
                        {
                            private final boolean isMultiPass = morePassesRequired.getAndSet( false );

                            @Override
                            public boolean visit( ReusableRelationship rel )
                            {
                                reportProgress( rel.id() );
                                if ( rel.inUse() )
                                {
                                    if(appendToRelChain( rel.getFirstNode(), rel.getFirstPrevRel(),
                                            rel.getFirstNextRel(), rel ))
                                    {
                                        return true;
                                    }

                                    if(appendToRelChain( rel.getSecondNode(), rel.getSecondPrevRel(),
                                            rel.getSecondNextRel(), rel ))
                                    {
                                        return true;
                                    }
                                }
                                return false;
                            }

                            private boolean appendToRelChain( long nodeId, long prevRel, long nextRel,
                                                              ReusableRelationship rel )
                            {
                                RelChain chain = relChains.get( nodeId );

                                if ( chain == null )
                                {
                                    if ( morePassesRequired.get() || (hasBeenProcessed( nodeId )) )
                                    {
                                        // Handled in a previous pass, ignore.
                                        return false;
                                    }

                                    if ( relChains.size() >= maxSimultaneousNodes )
                                    {
                                        morePassesRequired.set( true );
                                        firstRelationshipRequiringANewPass.set( rel.id() );
                                        System.out.print( "X" );
                                        return false;
                                    }

                                    chain = new RelChain( nodeId );
                                    relChains.put( nodeId, chain );
                                }

                                chain.append( rel.createRecord(), prevRel, nextRel );

                                if ( chain.isComplete() )
                                {
                                    assertNoWriterException( writerException );
                                    try
                                    {
                                        chainsToWrite.put( relChains.getAndRemove( nodeId ) );
                                    }
                                    catch ( InterruptedException e )
                                    {
                                        Thread.interrupted();
                                        throw new RuntimeException( "Interrupted while reading relationships.", e );
                                    }
                                }

                                return false;
                            }

                            private boolean hasBeenProcessed( long nodeId )
                            {
                                return false;//isMultiPass && nodeStore.inUse( nodeId );
                            }
                        } );

            } while(false && morePassesRequired.get());

            try
            {
                chainsToWrite.put( NodeProcessor.POISON );
                writerThread.join();
                assertNoWriterException( writerException );
            }
            catch ( InterruptedException e )
            {
                throw new RuntimeException( "Interrupted.", e);
            }
        }
        finally
        {
            reader.close();
        }
    }

    private void assertNoWriterException( AtomicReference<Throwable> writerException )
    {
        if(writerException.get() != null)
        {
            throw new RuntimeException( writerException.get() );
        }
    }

    private void reportProgress( long id )
    {
        int newPercent = totalEntities == 0 ? 100 : (int) ((id+1) * 100 / totalEntities);
        if ( newPercent > percentComplete )
        {
            percentComplete = newPercent;
            progressMonitor.percentComplete( percentComplete );
        }
    }
}

package org.neo4j.batch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.nioneo.store.Record;
import org.neo4j.kernel.impl.storemigration.monitoring.VisibleMigrationProgressMonitor;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.impl.util.collection.LongObjectOpenHashMap;
import org.neo4j.kernel.impl.util.collection.SimpleBitSet;

import static org.neo4j.batch.RelstoreReader.ReusableRelationship;

public class BatchProcess
{

    public static final DynamicRelationshipType FOLLOWS = DynamicRelationshipType.withName( "FOLLOWS" );

    private int percentComplete;
    private long totalEntities;
    private VisibleMigrationProgressMonitor progressMonitor;

    public static void main(String ... args) throws IOException, InterruptedException
    {
        long start = System.currentTimeMillis();
        new BatchProcess().run();
        long delta = System.currentTimeMillis() - start;
        System.out.println(delta / 1000.0);
    }

    private final int maxSimultaneousNodes = 12;//(int) (128 * (Runtime.getRuntime().totalMemory() / (1024 * 1024)));
    private final LongObjectOpenHashMap<RelChain> relChains = new LongObjectOpenHashMap<>( maxSimultaneousNodes );
    private final SimpleBitSet seenNodes = new SimpleBitSet( 1024 * 1024 * 1024 );
    private boolean morePassesRequired = false;
    private long firstRelationshipRequiringANewPass = 0l;
    private long numberOfPasses = 1;

    private void run() throws IOException
    {
        final RelstoreReader reader = new RelstoreReader(
                new File("/Users/jake/Code/neo4j-root/big-db/neostore.relationshipstore.db") );

        totalEntities = reader.getMaxId();
        progressMonitor = new VisibleMigrationProgressMonitor( StringLogger.SYSTEM, System.out );

        System.out.println("Processing " + totalEntities + " rels.");

        final ArrayBlockingQueue<RelChain>  unusedChains = new ArrayBlockingQueue<>( 1024 );
        final ArrayBlockingQueue<RelChain> chainsToWrite = new ArrayBlockingQueue<>( 1024 );
        final AtomicReference<Throwable> writerException = new AtomicReference<>();

        Thread writerThread = new NodeProcessor( chainsToWrite, unusedChains, writerException );
        writerThread.start();

        try
        {
            do
            {
                percentComplete = 0;
                if(morePassesRequired)
                {
                    System.out.println( "\n[MultiPass] Finished pass #" + (numberOfPasses-1) );
                    numberOfPasses++;
                }

                reader.accept( firstRelationshipRequiringANewPass,
                        new Visitor<ReusableRelationship, RuntimeException>()
                        {
                            private final boolean isMultiPass = morePassesRequired;

                            {
                                morePassesRequired = false;
                            }

                            @Override
                            public boolean visit( ReusableRelationship rel )
                            {
                                reportProgress( rel.id() );
                                if ( rel.inUse() )
                                {
                                    if(appendToRelChain( rel.getFirstNode(),
                                            rel.firstInFirstChain() ? Record.NO_PREV_RELATIONSHIP.intValue() : rel.getFirstPrevRel(), rel.getFirstNextRel(), rel ))
                                    {
                                        return true;
                                    }

                                    if(appendToRelChain( rel.getSecondNode(),
                                            rel.firstInSecondChain() ? Record.NO_PREV_RELATIONSHIP.intValue() : rel.getSecondPrevRel(),
                                            rel.getSecondNextRel(), rel ))
                                    {
                                        return true;
                                    }
                                }
                                return false;
                            }

                            private boolean appendToRelChain( long node, long prevRel, long nextRel,
                                                              ReusableRelationship rel )
                            {
                                RelChain chain = relChains.get( node );

                                if ( chain == null )
                                {
                                    if ( morePassesRequired || (isMultiPass && seenNodes.contains( (int) node )) )
                                    {
                                        // Handled in a previous pass, or wont fit, ignore.
                                        return false;
                                    }

                                    if ( relChains.size() >= maxSimultaneousNodes )
                                    {
                                        morePassesRequired = true;
                                        firstRelationshipRequiringANewPass = rel.id();
                                        System.out.print( "X" );
                                        return false;
                                    }

                                    seenNodes.put( (int)node ); // TODO: Needs to support longs
//                                    if((chain = unusedChains.poll()) == null)
//                                    {
                                        chain = new RelChain( node );
//                                    }
                                    relChains.put( node, chain );
                                }

                                chain.append( rel.id(), prevRel, nextRel, rel.createRecord()  );

                                if ( chain.isComplete() )
                                {
                                    assertNoWriterException( writerException );
                                    try
                                    {
                                        chainsToWrite.put( relChains.getAndRemove( node ) );
                                    }
                                    catch ( InterruptedException e )
                                    {
                                        Thread.interrupted();
                                        throw new RuntimeException( "Interrupted while reading relationships.", e );
                                    }
                                }

                                return false;
                            }

                        } );
                System.out.println();
                System.out.println("SIZE:" + relChains.size());
                relChains.visitValues( new Visitor<RelChain, RuntimeException>()
                {
                    @Override
                    public boolean visit( RelChain element ) throws RuntimeException
                    {
                        System.out.println(element);
                        return true;
                    }
                } );
            } while(morePassesRequired);

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
        while ( newPercent > percentComplete )
        {
            percentComplete++;
            progressMonitor.percentComplete( percentComplete );
        }
    }
}

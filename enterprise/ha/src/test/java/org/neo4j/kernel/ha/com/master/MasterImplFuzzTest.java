package org.neo4j.kernel.ha.com.master;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.neo4j.com.RequestContext;
import org.neo4j.com.ResourceReleaser;
import org.neo4j.com.Response;
import org.neo4j.com.TransactionNotPresentOnMasterException;
import org.neo4j.com.TransactionObligationResponse;
import org.neo4j.com.storecopy.StoreWriter;
import org.neo4j.kernel.IdType;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.ha.cluster.ConversationSPI;
import org.neo4j.kernel.ha.id.IdAllocation;
import org.neo4j.kernel.ha.lock.forseti.ForsetiLockManager;
import org.neo4j.kernel.impl.locking.AcquireLockTimeoutException;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.locking.ResourceTypes;
import org.neo4j.kernel.impl.store.StoreId;
import org.neo4j.kernel.impl.transaction.TransactionRepresentation;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.impl.util.JobScheduler;
import org.neo4j.kernel.impl.util.Neo4jJobScheduler;
import org.neo4j.kernel.impl.util.collection.ConcurrentAccessException;
import org.neo4j.kernel.lifecycle.LifeSupport;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.kernel.monitoring.Monitors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.neo4j.cluster.ClusterSettings.server_id;
import static org.neo4j.helpers.collection.MapUtil.stringMap;
import static org.neo4j.kernel.ha.HaSettings.lock_read_timeout;

public class MasterImplFuzzTest
{
    private static final int numWorkers = 10;
    private static final int numOperations = 100_000;
    private static final int numResources = 1000;

    public static final AtomicLong completedOperations = new AtomicLong();
    public static final AtomicLong alreadyInUseErrors = new AtomicLong();
    public static final AtomicLong transactionNotPresentErrors = new AtomicLong();
    public static final AtomicLong committedOperations = new AtomicLong();

    public static final StoreId StoreId = new StoreId();
    public static final Monitors monitors = new Monitors();
    public static final Random baseSourceOfRandom = new Random();

    private final LifeSupport life = new LifeSupport();
    private final ExecutorService executor = Executors.newFixedThreadPool( numWorkers + 1 );
    private final JobScheduler scheduler = life.add( new Neo4jJobScheduler() );
    private final MasterImpl.Monitor masterMonitor = monitors.newMonitor( MasterImpl.Monitor.class );
    private final Config config = new Config(stringMap( server_id.name(), "0", lock_read_timeout.name(), "1" ));
    private final Locks locks = new AssertingLockManager( new ForsetiLockManager( ResourceTypes.NODE, ResourceTypes.SCHEMA ) );

    @Test
    public void shouldHandleRandomizedLoad() throws Throwable
    {
        // Given
        final ConversationManager conversationManager = life.add( new ConversationManager( new AssertingConversationSPI( locks, scheduler ), config, 100, 0 ) );
        MasterImpl master = new MasterImpl( new AssertingMasterSPI(), conversationManager, masterMonitor, config );
        life.start();

        ConversationKiller conversationKiller = new ConversationKiller( conversationManager );
        new Thread( conversationKiller ).start();
        List<Future<Void>> workers = executor.invokeAll( workers( numWorkers, baseSourceOfRandom, master ) );

        // Wait for all workers to complete
        for ( Future<Void> future : workers )
        {
            future.get();
        }
        conversationKiller.stop();

        // Print stats
        System.out.println("Committed: " + committedOperations + " / " + completedOperations );
        System.out.println("AlreadyInUse: " + alreadyInUseErrors + " / " + completedOperations );
    }

    @After
    public void cleanup() throws InterruptedException
    {
        life.shutdown();
        executor.shutdownNow();
    }

    private List<Callable<Void>> workers( int numWorkers, Random baseSourceOfRandom, MasterImpl master )
    {
        LinkedList<Callable<Void>> workers = new LinkedList<>();
        for ( int i = 0; i < numWorkers; i++ )
        {
            workers.add( new SlaveEmulator( new Random( baseSourceOfRandom.nextInt() ), master ) );
        }
        return workers;
    }

    static class SlaveEmulator implements Callable<Void>
    {
        private static final AtomicInteger SlaveIdGenerator = new AtomicInteger(1);

        private final Random random;
        private final MasterImpl master;
        private final int machineId = SlaveIdGenerator.getAndIncrement();

        private State state = State.UNINITIALIZED;
        private long lastTx = 0;
        private long epoch;
        private RequestContext requestCtx;

        enum State
        {
            UNINITIALIZED
            {
                @Override
                State next( SlaveEmulator ctx )
                {
                    HandshakeResult handshake = ctx.master.handshake( ctx.lastTx, MasterImplFuzzTest.StoreId ).response();
                    ctx.epoch = handshake.epoch();
                    return IDLE;
                }
            },
            IDLE
            {
                @Override
                State next( SlaveEmulator ctx ) throws Exception
                {
                    if( oneInAHundredChance( ctx ) )
                    {
                        return UNINITIALIZED;
                    }
                    else if( oneInAHundredChance( ctx ))
                    {
                        return commit( ctx, new RequestContext( ctx.epoch, ctx.machineId, -1, ctx.lastTx, 0 ) );
                    }
                    else
                    {
                        try
                        {
                            ctx.master.newLockSession( ctx.newRequestContext() );
                            return IN_SESSION;
                        }
                        catch( TransactionFailureException e )
                        {
                            if( e.getCause() instanceof ConcurrentAccessException )
                            {
                                alreadyInUseErrors.incrementAndGet();
                                return IDLE;
                            }
                            else
                            {
                                throw e;
                            }
                        }
                    }
                }
            },
            IN_SESSION
            {
                @Override
                State next( SlaveEmulator ctx ) throws Exception
                {
                    if( oneInAHundredChance( ctx ) )
                    {
                        return UNINITIALIZED;
                    }
                    else
                    {
                        int i = ctx.random.nextInt( 10 );
                        if( i >= 5 )
                        {
                            return commit( ctx, ctx.requestCtx );
                        }
                        else if( i >= 4 )
                        {
                            ctx.master.acquireExclusiveLock( ctx.requestCtx, ResourceTypes.NODE, randomResource( ctx ) );
                            return IN_SESSION;
                        }
                        else if( i >= 1 )
                        {
                            ctx.master.acquireSharedLock( ctx.requestCtx, ResourceTypes.NODE, randomResource( ctx ) );
                            return IN_SESSION;
                        }
                        else
                        {
                            ctx.master.endLockSession( ctx.requestCtx, true );
                            return IDLE;
                        }
                    }
                }
            },
            CLOSING_SESSION
            {
                @Override
                State next( SlaveEmulator ctx ) throws Exception
                {
                    if( oneInAHundredChance( ctx ) )
                    {
                        return UNINITIALIZED;
                    }
                    else
                    {
                        ctx.master.endLockSession( ctx.requestCtx, true );
                        return IDLE;
                    }
                }
            };

            abstract State next( SlaveEmulator ctx ) throws Exception;

            protected State commit( SlaveEmulator ctx, RequestContext req )
                    throws IOException, TransactionFailureException
            {
                try
                {
                    ctx.master.commit( req, mock( TransactionRepresentation.class ) );
                    committedOperations.incrementAndGet();
                    return CLOSING_SESSION;
                }
                catch (TransactionNotPresentOnMasterException e )
                {
                    transactionNotPresentErrors.incrementAndGet();
                    return IDLE;
                }
            }
        }

        private static boolean oneInAHundredChance( SlaveEmulator ctx )
        {
            return ctx.random.nextInt( 100 ) <= 1;
        }

        private static long randomResource( SlaveEmulator ctx )
        {
            return ctx.random.nextInt( numResources );
        }

        public SlaveEmulator( Random random, MasterImpl master )
        {
            this.random = random;
            this.master = master;
        }

        @Override
        public Void call() throws Exception
        {
            for ( int i = 0; i < numOperations; i++ )
            {
                state = state.next( this );
            }
            return null;
        }

        RequestContext newRequestContext()
        {
            return requestCtx = new RequestContext( epoch, machineId, newLockSessionId(), lastTx, random.nextInt() );
        }

        private int newLockSessionId()
        {
            // If we based lock session id on a random seed, rather than depend directly on the lock client ids (which are pooled), request context clashes
            // become *much* less common. TODO: This is different from the real slave code
            return random.nextInt();
        }
    }

    static class AssertingMasterSPI implements MasterImpl.SPI
    {
        @Override
        public boolean isAccessible()
        {
            return true;
        }

        @Override
        public StoreId storeId()
        {
            return StoreId;
        }

        @Override
        public long applyPreparedTransaction( TransactionRepresentation preparedTransaction, Locks.Client locks ) throws IOException, TransactionFailureException
        {
            emulateDiskWrite();
            assertTrue( ((AssertingLockManager.AssertingLockClient) locks).open() );
            return 0;
        }

        private void emulateDiskWrite()
        {
            try
            {
                Thread.sleep( 5 );
            }
            catch ( InterruptedException e )
            {
                throw new RuntimeException( e );
            }
        }

        @Override
        public long getTransactionChecksum( long txId ) throws IOException
        {
            return 0;
        }

        @Override
        public <T> Response<T> packEmptyResponse( T response )
        {
            return new TransactionObligationResponse<>( response, StoreId, TransactionIdStore.BASE_TX_ID, ResourceReleaser.NO_OP );
        }

        @Override
        public <T> Response<T> packTransactionObligationResponse( RequestContext context, T response )
        {
            return packEmptyResponse( response );
        }

        @Override
        public IdAllocation allocateIds( IdType idType )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer createRelationshipType( String name )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public RequestContext flushStoresAndStreamStoreFiles( StoreWriter writer )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Response<T> packTransactionStreamResponse( RequestContext context, T response )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getOrCreateLabel( String name )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getOrCreateProperty( String name )
        {
            throw new UnsupportedOperationException();
        }
    }

    static class AssertingConversationSPI implements ConversationSPI
    {
        private final Locks locks;
        private final JobScheduler scheduler;

        public AssertingConversationSPI( Locks locks, JobScheduler scheduler )
        {
            this.locks = locks;
            this.scheduler = scheduler;
        }

        @Override
        public Locks.Client acquireClient()
        {
            return locks.newClient();
        }

        @Override
        public JobScheduler.JobHandle scheduleRecurringJob( JobScheduler.Group group, long interval, Runnable job )
        {
            // Scheduled much more often than necessary to trigger garbage collection of stale lock sessions constantly
            return scheduler.scheduleRecurring( group, job, 1, TimeUnit.MILLISECONDS );
        }
    }

    /** This emulates the MasterServer behavior of killing conversations after they have not had traffic sent on them for a certain time */
    private static class ConversationKiller implements Runnable
    {
        private volatile boolean running = true;
        private final ConversationManager conversationManager;

        public ConversationKiller( ConversationManager conversationManager )
        {
            this.conversationManager = conversationManager;
        }

        @Override
        public void run()
        {
            try
            {
                while ( running )
                {
                    Iterator<RequestContext> iterator = conversationManager.getActiveContexts().iterator();
                    if ( iterator.hasNext() )
                    {
                        RequestContext next = iterator.next();
                        conversationManager.end( next );
                    }
                    try
                    {
                        Thread.sleep( 1 );
                    }
                    catch ( InterruptedException e )
                    {
                        throw new RuntimeException( e );
                    }
                }
            } catch( Throwable e )
            {
                e.printStackTrace();
            }
        }

        public void stop()
        {
            running = false;
        }
    }

    private class AssertingLockManager extends LifecycleAdapter implements Locks
    {
        private final Locks delegate;

        public AssertingLockManager( Locks delegate )
        {
            this.delegate = delegate;
        }

        @Override
        public Client newClient()
        {
            final Locks.Client client = delegate.newClient();
            return new AssertingLockClient( client );
        }

        @Override
        public void accept( Visitor visitor )
        {

        }

        private class AssertingLockClient implements Client
        {
            private final Client client;
            public boolean open = true;

            public AssertingLockClient( Client client )
            {
                this.client = client;
            }

            @Override
            public void acquireShared( ResourceType resourceType, long resourceId ) throws AcquireLockTimeoutException
            {
                client.acquireShared( resourceType, resourceId );
            }

            @Override
            public void acquireExclusive( ResourceType resourceType, long resourceId ) throws AcquireLockTimeoutException
            {
                client.acquireExclusive( resourceType, resourceId );
            }

            @Override
            public boolean tryExclusiveLock( ResourceType resourceType, long resourceId )
            {
                return client.tryExclusiveLock( resourceType, resourceId );
            }

            @Override
            public boolean trySharedLock( ResourceType resourceType, long resourceId )
            {
                return client.trySharedLock( resourceType, resourceId );
            }

            @Override
            public void releaseShared( ResourceType resourceType, long resourceId )
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void releaseExclusive( ResourceType resourceType, long resourceId )
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void releaseAll()
            {
                client.releaseAll();
            }

            @Override
            public void close()
            {
                open = false;
                client.close();
            }

            public boolean open()
            {
                return open;
            }

            @Override
            public int getLockSessionId()
            {
                throw new UnsupportedOperationException();
            }
        }
    }
}
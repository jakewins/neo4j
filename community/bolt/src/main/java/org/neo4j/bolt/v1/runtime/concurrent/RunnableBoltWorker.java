/*
 * Copyright (c) 2002-2017 "Neo Technology,"
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
package org.neo4j.bolt.v1.runtime.concurrent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.neo4j.bolt.v1.runtime.BoltConnectionAuthFatality;
import org.neo4j.bolt.v1.runtime.BoltConnectionFatality;
import org.neo4j.bolt.v1.runtime.BoltProtocolBreachFatality;
import org.neo4j.bolt.v1.runtime.BoltStateMachine;
import org.neo4j.bolt.v1.runtime.BoltWorker;
import org.neo4j.bolt.v1.runtime.Job;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.logging.Log;

/**
 * Executes incoming Bolt requests for a given connection.
 */
class RunnableBoltWorker implements Runnable, BoltWorker
{
    private static final int workQueueSize = Integer.getInteger( "org.neo4j.bolt.workQueueSize", 100 );
    private static final long workQueueTimeout = Long.getLong( "org.neo4j.bolt.workQueueTimeoutMillis", TimeUnit.SECONDS.toMillis( 300 ) );

    private final ArrayBlockingQueue<Job> jobQueue = new ArrayBlockingQueue<>( workQueueSize );
    private final BoltStateMachine machine;
    private final Log log;
    private final Log userLog;

    private volatile boolean keepRunning = true;

    RunnableBoltWorker( BoltStateMachine machine, LogService logging )
    {
        this.machine = machine;
        this.log = logging.getInternalLog( getClass() );
        this.userLog = logging.getUserLog( getClass() );
    }

    /**
     * Accept a command to be executed at some point in the future. This will get queued and executed as soon as
     * possible.
     *
     * @param job an operation to be performed on the session
     */
    @Override
    public void enqueue( Job job )
    {
        try
        {
            if ( !keepRunning )
            {
                throw new RuntimeException( String.format( "Session %s has been halted, " + "cannot deliver message: %s", machine.key(), job ) );
            }
            boolean enqueued = jobQueue.offer( job, workQueueTimeout, TimeUnit.MILLISECONDS );
            if ( !enqueued )
            {
                // Very bad: the work queue for this worker has filled up. We *can't*
                // block on this queue any longer because this IO thread needs to get
                // back to processing other jobs, and we *cant* save the inbound message
                // because, well, the queue is bound for a reason - otherwise we'd run out
                // of RAM.
                // Hence:
                // We can't block here
                // We can't save the message
                // If we throw the message away, we can't accept more messages on this session,
                // since bolt does not handle message loss.
                // Hence, the session must die.

                // A better solution would be to provide TCP-level push-back; eg. not pulling
                // these message off of the network buffer at all..

                // The session will get killed, so to help debug, get the messages in the queue.
                // This is safe because no-one will put new messages on the queue other than the
                // IO thread that is executing this code path; as long as we ensure we never put
                // new message on the queue; we don't because the `halt` caused by the exception
                // thrown below will cause `keepRunning` to be false, which will cause the
                // branch at the beginning of this function to trigger.
                String queueContents = ArrayBlockingQueueDescriber.describe( jobQueue );
                log.error( "Session %s, currently in state %s has reached workQueueSize (%d) and " +
                                "adding to the queue timed out; message  must be dropped to keep from " +
                                "unbound memory growth, meaning the session must be terminated. " + "New message was: %s. " + "Messages in queue were: %s",
                        machine.key(), machine.state(), workQueueSize, job.toString(), queueContents );
                // TODO Why are we not throwing KernelExceptions or some other checked exception here?
                // This will be caught by BoltProtocolV1#handle, which will call #halt on this worker
                throw new RuntimeException( String.format( "Enqueuing message on Session %s timed out, " + "see log for details.", machine.key() ) );
            }
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException( String.format( "Worker interrupted while queueing request on session %s, the session may have been " +
                    "forcibly closed, or the database may be shutting down.", machine.key() ) );
        }
    }

    @Override
    public void run()
    {
        List<Job> batch = new ArrayList<>( workQueueSize );

        try
        {
            while ( keepRunning )
            {
                Job job = jobQueue.poll( 10, TimeUnit.SECONDS );
                if ( job != null )
                {
                    execute( job );

                    for ( int jobCount = jobQueue.drainTo( batch ); keepRunning && jobCount > 0; jobCount = jobQueue.drainTo( batch ) )
                    {
                        executeBatch( batch );
                    }
                }
            }
        }
        catch ( BoltConnectionAuthFatality e )
        {
            // this is logged in the SecurityLog
        }
        catch ( BoltProtocolBreachFatality e )
        {
            log.error( "Bolt protocol breach in session '" + machine.key() + "'", e );
        }
        catch ( Throwable t )
        {
            userLog.error( "Worker for session '" + machine.key() + "' crashed.", t );
        }
        finally
        {
            closeStateMachine();
        }
    }

    private void executeBatch( List<Job> batch ) throws BoltConnectionFatality
    {
        for ( int i = 0; keepRunning && i < batch.size(); i++ )
        {
            execute( batch.get( i ) );
        }
        batch.clear();
    }

    private void execute( Job job ) throws BoltConnectionFatality
    {
        job.perform( machine );
    }

    @Override
    public void interrupt()
    {
        machine.interrupt();
    }

    @Override
    public void halt()
    {
        try
        {
            // Notify the state machine that it should terminate.
            // We can't close it here because this method can be called from a different thread.
            // State machine will be closed when this worker exits.
            machine.terminate();
        }
        finally
        {
            keepRunning = false;
        }
    }

    private void closeStateMachine()
    {
        try
        {
            // Attempt to close the state machine, as an effort to release locks and other resources
            machine.close();
        }
        catch ( Throwable t )
        {
            log.error( "Unable to close Bolt session '" + machine.key() + "'", t );
        }
    }
}

class ArrayBlockingQueueDescriber
{
    // Best-effort introspection to describe contents of queue; I don't think
    // we should move this into the main code base as the effect of this
    // on JVM optimizations are presumably bad. Want it in here to find out
    // what is in this queue.
    static String describe( ArrayBlockingQueue<Job> queue )
    {
        StringBuilder out = new StringBuilder();
        try
        {
            Field itemsField = ArrayBlockingQueue.class.getDeclaredField( "items" );
            Field countField = ArrayBlockingQueue.class.getDeclaredField( "count" );
            Field takeIndexField = ArrayBlockingQueue.class.getDeclaredField( "takeIndex" );

            itemsField.setAccessible( true );
            countField.setAccessible( true );
            takeIndexField.setAccessible( true );

            Object[] items = (Object[]) itemsField.get( queue );
            int n = countField.getInt( queue );
            int take = takeIndexField.getInt( queue );
            int i = 0;
            out.append( "[" );
            while ( i < n )
            {
                if ( i > 0 )
                {
                    out.append( ", " );
                }

                Object x = items[take];

                out.append( x );

                if ( ++take == items.length )
                {
                    take = 0;
                }
                i++;
            }
            out.append( "]" );
            return out.toString();
        }
        catch ( Throwable e )
        {
            return String.format( "[Unable to describe queue: %s]", e.getMessage() );
        }
    }
}

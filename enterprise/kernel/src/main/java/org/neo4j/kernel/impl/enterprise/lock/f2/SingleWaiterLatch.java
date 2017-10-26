/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.enterprise.lock.f2;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * A specialized semaphore optimized for a single waiter; it works roughly the same as
 * {@link java.util.concurrent.Semaphore}, but because it does not maintain a wait list it can be substantially
 * faster (eg. about one order faster for ping/pong micro benchmark).
 */
public class SingleWaiterLatch
{

    private static final int OPEN = 0; // or less; negatives indicate additional permits to acquire
    private static final int CLOSED = 1;
    private static final int CLOSED_WAITING_FOR_SIGNAL = 2;

    private final AtomicInteger state = new AtomicInteger( CLOSED );
    private Thread waiter;

    public boolean tryAcquire( long time, TimeUnit unit ) throws InterruptedException
    {
        long deadline = -1;
        int yieldIterations = 100;
        for ( ; ; )
        {
            int currentState = state.get();
            if ( currentState <= OPEN && state.compareAndSet( currentState, currentState + 1 ) )
            {
                return true;
            }

            if ( currentState == CLOSED_WAITING_FOR_SIGNAL )
            {
                // At most one waiter at a time is allowed
                return false;
            }

            if ( yieldIterations > 0 )
            {
                yieldIterations--;
                Thread.yield();
                continue;
            }

            if ( deadline == -1 )
            {
                deadline = System.nanoTime() + unit.toNanos( time );
            }

            long waitNanos = deadline - System.nanoTime();
            if ( waitNanos <= 0 )
            {
                return false;
            }

            waiter = Thread.currentThread();
            try
            {
                if ( state.compareAndSet( CLOSED, CLOSED_WAITING_FOR_SIGNAL ) )
                {
                    LockSupport.parkNanos( waitNanos );

                    currentState = state.get();
                    if ( currentState == CLOSED_WAITING_FOR_SIGNAL )
                    {
                        state.compareAndSet( CLOSED_WAITING_FOR_SIGNAL, CLOSED );
                    }
                }
            }
            finally
            {
                waiter = null;
            }
        }
    }

    public void release()
    {
        for ( ; ; )
        {
            int currentState = state.get();
            if ( currentState == CLOSED )
            {
                if ( state.compareAndSet( CLOSED, OPEN ) )
                {
                    return;
                }
            }
            else if ( currentState == CLOSED_WAITING_FOR_SIGNAL )
            {
                if ( state.compareAndSet( CLOSED_WAITING_FOR_SIGNAL, OPEN ) )
                {
                    LockSupport.unpark( waiter );
                    return;
                }
            }
            else
            {
                // If the lock is not closed, we simply add another permit to acquire the lock
                if ( state.compareAndSet( currentState, currentState - 1 ) )
                {
                    return;
                }
            }
        }
    }
}

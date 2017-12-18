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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.neo4j.kernel.impl.locking.Locks;

enum LockMode
{
    /** Client wants to ensure exclusive access to the resource */
    EXCLUSIVE( 0 ),
    /** Client wants to ensure nobody access to the resource, willing to share it with others who feel the same way. */
    SHARED( 1 ),
    /** Client holds a share lock, wants to upgrade it to exclusive access */
    UPGRADE( 2 ),
    /** Client holds no lock, useful for some control flows */
    NONE( 4 );

    final int index;

    LockMode( int index )
    {
        this.index = index;
    }

    public static int numberOfModes()
    {
        return LockMode.values().length;
    }
}

enum AcquireMode
{
    BLOCKING,
    NONBLOCKING
}

class LockGraphDump
{

    private static Set<F2Partitions> lockManagers = new HashSet<>();
    private static Set<F2Client> clients = new HashSet<>();

    static
    {
//        Signal.handle(new Signal("URG"), signal -> {
//            for(F2Partitions partitions : lockManagers) {
//                partitions.stopTheWorld();
//                try {
//                    for (int i = 0; i < partitions.numberOfPartitions(); i++) {
//                        F2Partition partition = partitions.getPartitionByIndex(i);
//                        partition.activeLocks().forEach((lock) -> {
//                            String lockHolder;
//                            if(lock.exclusiveHolder != null) {
//                                lockHolder = String.format("[(exclusive) %s]", lock.exclusiveHolder.owner);
//                            } else {
//                                StringBuilder sb = new StringBuilder("[(shared) ");
//                                for(F2ClientEntry entry=lock.sharedHolderList; entry != null; entry = entry.next) {
//                                    sb.append(entry.owner);
//                                    if(entry.next != null) {
//                                        sb.append(", ");
//                                    }
//                                }
//                                sb.append("]");
//                                lockHolder = sb.toString();
//                            }
//                            for(F2ClientEntry entry = lock.waitList; entry != null; entry = entry.next ) {
//                                System.err.printf("[%s] is waiting for %s to get %s\n", entry.owner, lockHolder, entry);
//                            }
//                        });
//                    }
//
//                    System.err.println("");
//
//                    for (F2Client client : clients) {
//                        System.err.printf("%s: %s\n", client, client.waitsFor);
//                    }
//
//                } finally {
//                    partitions.resumeTheWorld();
//                }
//            }
//        });
    }

    static synchronized void register( F2Partitions partitions )
    {
        lockManagers.add( partitions );
    }

    static synchronized void register( F2Client client )
    {
        clients.add( client );
    }

    static synchronized void unregister( F2Partitions partitions )
    {
        lockManagers.remove( partitions );
    }
}

public class F2Locks implements Locks
{
    private final F2Partitions partitions;
    private final DeadlockDetector deadlockDetector;
    private final int numResourceTypes;
    private AtomicLong clientCounter = new AtomicLong();

    public F2Locks( int numResourceTypes, int numPartitions )
    {
        this.numResourceTypes = numResourceTypes;
        this.partitions = new F2Partitions( numResourceTypes, numPartitions );
        this.deadlockDetector = new DeadlockDetector();

        LockGraphDump.register( this.partitions );
    }

    @Override
    public Client newClient()
    {
        F2Client client = new F2Client( numResourceTypes, partitions, deadlockDetector );
        client.setName( String.format( "%d", clientCounter.getAndIncrement() ) );
        LockGraphDump.register( client );
        return client;
    }

    @Override
    public void accept( Visitor visitor )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close()
    {
        LockGraphDump.unregister( this.partitions );
    }
}


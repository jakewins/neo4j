/**
 * Copyright (c) 2002-2012 "Neo Technology,"
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
package org.neo4j.server.webadmin.profiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.neo4j.cypher.internal.LRUCache;
import org.neo4j.kernel.info.Monitors;

public class Profiler implements LRUCache.Monitor
{
    private final Monitors monitors;
    private final BoundedList events;
    private final Set<String> listeners = new HashSet<String>();

    public Profiler( Monitors monitors )
    {
        this(monitors, 1000);
    }

    public Profiler( Monitors monitors, int limit )
    {
        this.monitors = monitors;
        this.events = new BoundedList(limit);
    }

    public void startListening( String id )
    {
        listeners.add( id );

        if ( listeners.size() > 0 )
        {
            monitors.addMonitorListener( this );
        }
    }

    public void stopListening( String id )
    {
        listeners.remove( id );
        if ( listeners.size() == 0 )
        {
            monitors.removeMonitorListener( this );
            events.clear();
        }
    }

    public Iterable<Event> getEventsSince( long since )
    {
        return events.getEventsSince( since );
    }

    public void usedCachedQuery( String query )
    {
        events.add( new QueryCacheEvent( System.currentTimeMillis(), 0, query, true ) );
    }

    public void parsedQuery( String query, long timeTaken )
    {
        events.add( new QueryCacheEvent( System.currentTimeMillis(), timeTaken, query, false ) );
    }

    private class BoundedList
    {
        private final List<Event> inner = new ArrayList<Event>();
        private final ReentrantLock lock = new ReentrantLock();
        private final int limit;

        public BoundedList( int limit )
        {
            this.limit = limit;
        }

        public Iterable<Event> getEventsSince( long time )
        {
            lock.lock();
            try
            {
                ArrayList<Event> result = new ArrayList<Event>();
                for ( Event event : inner )
                {
                    if ( event.getTime() > time )
                    {
                        result.add( event );
                    }
                }
                return result;

            }
            finally
            {
                lock.unlock();
            }
        }

        public void clear()
        {
            lock.lock();
            try
            {
                inner.clear();
            }
            finally
            {
                lock.unlock();
            }

        }

        public void add( Event event )
        {
            lock.lock();
            try
            {
                inner.add( event );
                if(inner.size()>limit)
                {
                    inner.remove( 0 );
                }
            }
            finally
            {
                lock.unlock();
            }
        }
    }
}

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
package org.neo4j.server.webadmin;

import static junit.framework.Assert.assertFalse;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.neo4j.helpers.collection.IteratorUtil.count;

import java.util.Iterator;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.internal.LRUCache;
import org.neo4j.kernel.info.Monitors;
import org.neo4j.server.webadmin.profiler.Event;
import org.neo4j.server.webadmin.profiler.Profiler;
import org.neo4j.server.webadmin.profiler.QueryCacheEvent;

public class ProfilerTest
{
    private Monitors monitor;
    private Profiler service;

    @Before
    public void init()
    {
        monitor = new Monitors();
        service = new Profiler( monitor );
    }

    @Test
    public void shouldBeAbleToStartListening() throws Exception
    {
        // GIVEN
        service.startListening("me");

        // WHEN
        Iterable<Event> events = service.getEventsSince( 0 );

        // THEN
        assertThat( count( events ), is( 0 ) );
    }

    @Test
    public void shouldSeeSingleEvent() throws Exception
    {
        // GIVEN
        LRUCache.Monitor cacheMonitor = createCacheMonitor();
        service.startListening("me");
        cacheMonitor.usedCachedQuery( "HELLO WORLD!" );

        // WHEN
        Iterator<Event> events = service.getEventsSince( 0 ).iterator();

        // THEN
        assertTrue( "Didn't see the cached query", events.hasNext() );
        QueryCacheEvent event = (QueryCacheEvent) events.next();
        assertTrue( "Expected the query to be marked as cached", event.isCached() );
        assertThat( event.getQuery(), is( "HELLO WORLD!" ) );
    }

    @Test
    public void shouldNotSeeEventsHappeningBefore() throws Exception
    {
        // GIVEN
        LRUCache.Monitor cacheMonitor = createCacheMonitor();
        cacheMonitor.usedCachedQuery( "HELLO WORLD!" );

        service.startListening("me");

        // WHEN
        Iterator<Event> events = service.getEventsSince( 0 ).iterator();

        // THEN
        assertFalse( "Should not have seen this event", events.hasNext() );
    }

    @Test
    public void shouldStopRecordingWhenNoOneIsListening() throws Exception
    {
        // GIVEN
        LRUCache.Monitor cacheMonitor = createCacheMonitor();
        service.startListening("me");
        cacheMonitor.usedCachedQuery( "HELLO WORLD!" );


        // WHEN
        service.stopListening( "me" );

        // THEN
        Iterator<Event> events = service.getEventsSince( 0 ).iterator();
        assertFalse( "Should not have seen this event", events.hasNext() );
    }

    @Test
    public void shouldNotShowOldEvents() throws Exception
    {
        // GIVEN
        LRUCache.Monitor cacheMonitor = createCacheMonitor();
        service.startListening("me");

        cacheMonitor.usedCachedQuery( "HELLO WORLD!" );


        // WHEN
        long now = System.currentTimeMillis();

        // THEN
        Iterator<Event> events = service.getEventsSince( now + 100 ).iterator();
        assertFalse( "Should not have seen this event", events.hasNext() );
    }

    @Test
    public void shouldReturnEventsInDescendingTimeOrder() throws Exception
    {
        // GIVEN
        LRUCache.Monitor cacheMonitor = createCacheMonitor();
        service.startListening("me");
        cacheMonitor.usedCachedQuery( "ONE" );
        cacheMonitor.usedCachedQuery( "TWO" );

        // WHEN
        Iterator<Event> events = service.getEventsSince( 0 ).iterator();

        // THEN
        assertTrue("Latest query comes first in results", ((QueryCacheEvent)events.next()).getQuery().equals("TWO"));
        assertTrue("Oldest query comes last in results", ((QueryCacheEvent)events.next()).getQuery().equals("ONE"));
    }



    private LRUCache.Monitor createCacheMonitor()
    {
        return monitor.newMonitor( LRUCache.Monitor.class );
    }
}

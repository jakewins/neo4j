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
package org.neo4j.kernel.info;

import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;
import org.junit.Test;

public class MonitorsTest
{
    @Test
    public void testMonitor()
    {
        Monitors monitors = new Monitors();

        MyMonitor monitor = monitors.newMonitor( MyMonitor.class );

        final AtomicReference<String> val = new AtomicReference<String>();

        MyMonitor testMonitor = new MyMonitor()
        {
            @Override
            public void testMonitor( String arg )
            {
                val.set( arg );
            }
        };

        monitors.addMonitorListener( testMonitor );
        monitor.testMonitor( "foo" );
        Assert.assertEquals( "foo", val.get() );

        val.set( null );
        monitors.removeMonitorListener( testMonitor );
        monitor.testMonitor( "foo" );

        Assert.assertNull( val.get() );
    }

    public interface MyMonitor
    {
        void testMonitor( String arg );
    }
}

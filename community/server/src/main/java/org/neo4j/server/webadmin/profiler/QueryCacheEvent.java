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

public class QueryCacheEvent implements Event
{
    private final long time;
    private final long timeTaken;
    private final String query;
    private final boolean cached;

    public QueryCacheEvent( long time, long timeTaken, String query, boolean cached )
    {
        this.time = time;
        this.timeTaken = timeTaken;
        this.query = query;
        this.cached = cached;
    }

    @Override
    public long getTime()
    {
        return time;
    }

    public long getTimeTaken()
    {
        return timeTaken;
    }

    public String getQuery()
    {
        return query;
    }

    public boolean isCached()
    {
        return cached;
    }
}

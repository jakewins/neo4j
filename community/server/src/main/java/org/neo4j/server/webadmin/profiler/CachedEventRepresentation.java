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

import static org.neo4j.server.rest.repr.ValueRepresentation.bool;
import static org.neo4j.server.rest.repr.ValueRepresentation.number;
import static org.neo4j.server.rest.repr.ValueRepresentation.string;

import org.neo4j.server.rest.repr.ObjectRepresentation;
import org.neo4j.server.rest.repr.ValueRepresentation;

public class CachedEventRepresentation extends ObjectRepresentation
{
    private final QueryCacheEvent event;

    public CachedEventRepresentation( QueryCacheEvent event )
    {
        super( "cachedEventRepresentation" );
        this.event = event;
    }

    @Mapping("timestamp")
    public ValueRepresentation getTimeStamp()
    {
        return number( event.getTime() );
    }

    @Mapping("query")
    public ValueRepresentation getQuery()
    {
        return string( event.getQuery() );
    }

    @Mapping("wasCached")
    public ValueRepresentation isCached()
    {
        return bool( event.isCached() );
    }

    @Mapping("parseTime")
    public ValueRepresentation getTimeTaken()
    {
        return number( event.getTimeTaken() );
    }
}

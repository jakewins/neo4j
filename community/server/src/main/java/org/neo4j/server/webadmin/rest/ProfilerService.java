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
package org.neo4j.server.webadmin.rest;


import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.neo4j.server.rest.repr.ListRepresentation;
import org.neo4j.server.rest.repr.OutputFormat;
import org.neo4j.server.webadmin.profiler.CachedEventRepresentation;
import org.neo4j.server.webadmin.profiler.Event;
import org.neo4j.server.webadmin.profiler.Profiler;
import org.neo4j.server.webadmin.profiler.QueryCacheEvent;

@Path(ProfilerService.ROOT_PATH)
public class ProfilerService implements AdvertisableService
{
    public static final String ROOT_PATH = "server/profile";
    public static final String START_PROFILING = "/start";
    public static final String STOP_PROFILING = "/stop";
    public static final String GET_EVENTS = "/fetch/{since}";

    private final Profiler profiler;

    public ProfilerService( @Context Profiler profiler )
    {
        this.profiler = profiler;
    }

    @PUT
    @Path(START_PROFILING)
    public Response startListening( @Context HttpServletRequest req, @Context OutputFormat output )
    {
        profiler.startListening( req.getSession().getId() );
        return output.noContent();
    }

    @PUT
    @Path(STOP_PROFILING)
    public Response stopListening( @Context HttpServletRequest req, @Context OutputFormat output )
    {
        profiler.stopListening( req.getSession().getId() );
        return output.noContent();
    }

    @GET
    @Path(GET_EVENTS)
    public Response getEventsSince( @PathParam("since") Long since, @Context OutputFormat output )
    {
        Iterable<Event> events = profiler.getEventsSince( since );

        ArrayList<CachedEventRepresentation> eventRepresentations = new ArrayList<CachedEventRepresentation>();
        for ( Event event : events )
        {
            eventRepresentations.add( new CachedEventRepresentation( (QueryCacheEvent) event ) );
        }
        return output.ok( new ListRepresentation( "cachedEventRepresentation", eventRepresentations ) );
    }


    @Override
    public String getName()
    {
        return "profiler";
    }

    @Override
    public String getServerPath()
    {
        return ROOT_PATH;
    }
}

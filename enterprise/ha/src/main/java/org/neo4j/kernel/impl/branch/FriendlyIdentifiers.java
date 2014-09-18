/**
 * Copyright (c) 2002-2014 "Neo Technology,"
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
package org.neo4j.kernel.impl.branch;

import java.util.HashMap;
import java.util.Map;

public class FriendlyIdentifiers
{
    private int nodeIdentifierCounter = 1;
    private int relIdentifierCounter = 1;

    private Map<Long, String> assignedNodeIdentifiers = new HashMap<>();
    private Map<Long, String> assignedRelIdentifiers = new HashMap<>();

    String node(long id)
    {
        String identifier = assignedNodeIdentifiers.get( id );
        if(identifier == null)
        {
            identifier = "n" + nodeIdentifierCounter++;
            assignedNodeIdentifiers.put( id, identifier );
        }
        return identifier;
    }

    public String rel( long id )
    {
        String identifier = assignedRelIdentifiers.get( id );
        if(identifier == null)
        {
            identifier = "n" + relIdentifierCounter++;
            assignedRelIdentifiers.put( id, identifier );
        }
        return identifier;
    }
}

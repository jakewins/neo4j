/**
 * Copyright (c) 2002-2014 "Neo Technology,"
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
package org.neo4j.kernel.impl.api.store;

import org.neo4j.collection.primitive.PrimitiveLongIterator;
import org.neo4j.cursor.Cursor;
import org.neo4j.graphdb.Direction;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.impl.api.RelationshipVisitor;
import org.neo4j.register.Register;

public class StoreTraverseCursor implements Cursor
{
    private final CacheLayer store;
    private final Cursor inputCursor;
    private final Register.Int64.Read nodeId;
    private final Register.Obj.Read<int[]> relTypes;
    private final Register.Obj.Read<Direction> direction;
    private final Register.Int64.Write relId;
    private final Register.Int64.Write neighborNodeId;

    private final RelationshipVisitor<RuntimeException> neighborFetcher = new RelationshipVisitor<RuntimeException>()
    {
        @Override
        public void visit( long relId, long startNode, long endNode, int type ) throws RuntimeException
        {
            neighborNodeId.write( startNode == nodeId.read() ? endNode : startNode );
        }
    };

    private PrimitiveLongIterator relIterator;

    public StoreTraverseCursor( CacheLayer cacheLayer, Cursor inputCursor, Register.Int64.Read nodeId,
                                Register.Obj.Read<int[]> relTypes,
                                Register.Obj.Read<Direction> direction, Register.Int64.Write relId,
                                Register.Int64.Write neighborNodeId )
    {
        this.store = cacheLayer;
        this.inputCursor = inputCursor;
        this.nodeId = nodeId;
        this.relTypes = relTypes;
        this.direction = direction;
        this.relId = relId;
        this.neighborNodeId = neighborNodeId;
    }

    @Override
    public boolean next()
    {
        if(relIterator == null)
        {
            if ( !nextInputNode() )
            {
                return false;
            }
        }

        if(relIterator.hasNext())
        {
            return nextRelationship();
        }
        else
        {
            relIterator = null;
            return next();
        }
    }

    private boolean nextRelationship()
    {
        long next = relIterator.next();
        relId.write( next );
        try
        {
            store.relationshipVisit( next, neighborFetcher );
            return true;
        }
        catch ( EntityNotFoundException e )
        {
            return next();
        }
    }

    private boolean nextInputNode()
    {
        if( inputCursor.next() )
        {
            try
            {
                relIterator = store.nodeListRelationships( nodeId.read(), direction.read(), relTypes.read() );
                return true;
            }
            catch ( EntityNotFoundException e )
            {
                return nextInputNode();
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    public void reset()
    {
        inputCursor.reset();
    }

    @Override
    public void close()
    {
        inputCursor.close();
    }
}

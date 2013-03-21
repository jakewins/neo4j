/**
 * Copyright (c) 2002-2013 "Neo Technology,"
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
package org.neo4j.kernel.impl.api;

import org.neo4j.helpers.Function;
import org.neo4j.kernel.api.ConstraintViolationKernelException;
import org.neo4j.kernel.api.operations.SchemaOperations;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;

public class SchemaStateOperations extends DelegatingSchemaOperations
{
    private final SchemaState schemaState;

    public SchemaStateOperations( SchemaOperations inner, SchemaState schemaState)
    {
        super( inner );
        this.schemaState = schemaState;
    }

    @Override
    public IndexRule addIndexRule( long labelId, long propertyKey ) throws ConstraintViolationKernelException
    {
        // schemaState.flush() is called only when the index actually goes online
        return delegate.addIndexRule( labelId, propertyKey );
    }

    @Override
    public void dropIndexRule( IndexRule indexRule ) throws ConstraintViolationKernelException
    {
        schemaState.flush();
        delegate.dropIndexRule( indexRule );
    }

    @Override
    public <K, V> V getOrCreateFromSchemaState( K key, Function<K, V> creator )
    {
        return schemaState.getOrCreate( key, creator );
    }

    @Override
    public <K> boolean schemaStateContains( K key )
    {
        return null != schemaState.get( key );
    }
}

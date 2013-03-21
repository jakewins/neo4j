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

import java.util.HashMap;
import java.util.Map;

import org.neo4j.helpers.Function;

public class TransactionalSchemaStateImpl implements TransactionalSchemaState
{
    private final SchemaStateStore delegate;

    private Map<Object, Object> map = new HashMap<Object, Object>();
    private boolean wasFlushed = false;
    private boolean wasCommited = false;

    public TransactionalSchemaStateImpl( SchemaStateStore delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public <K, V> V getOrCreate( K key, Function<K, V> creator )
    {
        ensureNotCommitted();
        if ( map.containsKey( key ) )
        {
            return (V) map.get( key );
        }
        else {
            if (wasFlushed)
            {
                V value = creator.apply( key );
                map.put( key, value );
                return value;
            }
            else {
                V value = delegate.get( key );
                if ( value == null )
                {
                    V newValue = creator.apply( key );
                    map.put( key, newValue );
                    return newValue;
                }
                else
                {
                    return value;
                }
            }
        }
    }

    @Override
    public <K, V> V get( K key )
    {
        ensureNotCommitted();
        if(map.containsKey( key ))
        {
            return (V) map.get( key );
        }
        return delegate.get( key );
    }

    @Override
    public void flush()
    {
        ensureNotCommitted();
        wasFlushed = true;
        map.clear();
    }

    @Override
    public void commit()
    {
        try
        {
            if (wasFlushed)
            {
                delegate.flush();
            }
            delegate.apply( map );
        }
        finally {
            wasFlushed = false;
            wasCommited = true;
        }
    }

    private void ensureNotCommitted()
    {
        if (wasCommited)
            throw new IllegalStateException( getClass().getSimpleName() + " has already been commited" );
    }
}

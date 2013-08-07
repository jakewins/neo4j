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
package org.neo4j.kernel.impl.pool;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * An object pool that expects single-threaded access.
 */
public class SingleThreadedPool<T extends Pool.Item> implements Pool<T>
{
    private final Queue<T> pool;
    private final Item.Factory<T> factory;

    /**
     * Returns items to our pool.
     */
    private final Releaser releaser = new Releaser(){
        @Override
        public void release( Item obj )
        {
            pool.add( (T) obj );
        }
    };

    /**
     * Releases items that are allocated when the pool has been drained - these items are not
     * returned to the pool, but thrown away when they are released. We may want to re-visit
     * this approach.
     */
    private final Releaser extraItemReleaser = new Releaser()
    {
        @Override
        public void release( Item obj )
        {
            factory.destroyItem( (T) obj );
        }
    };

    public SingleThreadedPool( int size, Item.Factory<T> factory )
    {
        this.factory = factory;
        this.pool = new ArrayDeque<>( size );

        // Prime the pool
        for(int i=0;i<size;i++)
        {
            pool.add( factory.newItem( releaser ) );
        }
    }

    @Override
    public T claim()
    {
        T item = pool.poll();
        if( item == null )
        {
            return factory.newItem( extraItemReleaser );
        }

        return item;
    }
}

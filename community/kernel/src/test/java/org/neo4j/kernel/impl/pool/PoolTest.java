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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class PoolTest
{

    public static class MyThing implements Pool.Item
    {
        private final Pool.Releaser poolReleaser;

        public MyThing( Pool.Releaser poolReleaser )
        {
            this.poolReleaser = poolReleaser;
        }

        @Override
        public void release()
        {
            poolReleaser.release( this );
        }
    }

    private static class MyThingFactory implements Pool.Item.Factory<MyThing>
    {
        @Override
        public MyThing newItem( Pool.Releaser releaser )
        {
            return new MyThing( releaser );
        }

        @Override
        public void destroyItem( MyThing obj )
        {
        }
    }

    public abstract Pool<MyThing> newPool( int size, Pool.Item.Factory<MyThing> factory );

    @Test
    public void shouldAllocateObjects() throws Exception
    {
        // Given
        Pool<MyThing> pool = newPool(1, new MyThingFactory());

        // When
        MyThing thing = pool.claim();
        thing.release();
        MyThing thingAgain = pool.claim();

        // Then
        assertThat( thing, equalTo( thingAgain ) );
    }

    @Test
    public void shouldAllocateOffPoolItemsIfPoolIsExhausted() throws Exception
    {
        // Given
        Pool<MyThing> pool = newPool(1, new MyThingFactory());

        // When
        MyThing thing      = pool.claim();
        MyThing thingAgain = pool.claim();

        // Then
        assertThat( thing, not( equalTo( thingAgain ) ) );
    }

}

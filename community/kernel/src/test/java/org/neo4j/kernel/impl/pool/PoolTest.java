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

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

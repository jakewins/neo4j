package org.neo4j.kernel.impl.pool;

/**
 * An object pool API, inspired by Stormpot
 */
public interface Pool<T extends Pool.Item>
{

    interface Item
    {
        interface Factory<T extends Item>
        {
            T newItem( Releaser releaser );
            void destroyItem( T obj );
        }

        void release();
    }

    interface Releaser
    {
        void release( Item obj );
    }

    T claim();

}

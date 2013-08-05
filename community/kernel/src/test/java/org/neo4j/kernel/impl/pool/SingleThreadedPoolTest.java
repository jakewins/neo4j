package org.neo4j.kernel.impl.pool;

public class SingleThreadedPoolTest extends PoolTest
{
    @Override
    public Pool<MyThing> newPool( int size, Pool.Item.Factory<MyThing> factory )
    {
        return new SingleThreadedPool<>( size, factory );
    }
}

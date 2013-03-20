package org.neo4j.kernel.impl.api;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.helpers.Function;

public class TransactionAwareSchemaStateHolder implements SchemaStateHolder
{
    private final UpdateableSchemaStateHolder delegate;

    private Map<Object, Object> map = new HashMap<Object, Object>();
    private boolean wasFlushed = false;
    private boolean wasCommited = false;

    public TransactionAwareSchemaStateHolder( UpdateableSchemaStateHolder delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public <K, V> V getOrCreate( K key, Class<V> clazz, Function<K, V> creator )
    {
        ensureNotCommitted();
        return getOrCreateAndPut( key, clazz, creator, map );
    }

    @Override
    public <K, V> V getOrCreateAndPut( K key, Class<V> clazz, Function<K, V> creator, Map<Object, Object> targetMap )
    {
        ensureNotCommitted();
        if ( map.containsKey( key ) )
        {
            return clazz.cast( map.get( key ) );
        }
        else {
            if (wasFlushed)
            {
                V value = creator.apply( key );
                targetMap.put( key, value );
                return value;
            }
            else
                return delegate.getOrCreateAndPut( key, clazz, creator, targetMap );
        }
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

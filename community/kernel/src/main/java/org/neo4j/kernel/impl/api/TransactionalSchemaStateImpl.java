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

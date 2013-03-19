package org.neo4j.kernel.impl.api;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.helpers.Function;

public class TransactionAwareSchemaStateHolder implements SchemaStateHolder
{
    private final KernelSchemaStateHolder holder;

    private final Map<Object, Object> map = new HashMap<Object, Object>();
    private boolean wasFlushed = false;

    public TransactionAwareSchemaStateHolder( KernelSchemaStateHolder holder )
    {
        this.holder = holder;
    }

    @Override
    public <K, V> V getOrCreate( K key, Class<V> clazz, Function<K, V> creator )
    {
        if ( map.containsKey( key ) )
            return clazz.cast( map.get( key ) );
        else
            return holder.getOrCreateAndPut( key, clazz, creator, map );
    }

    @Override
    public void flush()
    {
        wasFlushed = true;
        map.clear();
    }

    @Override
    public void commit()
    {
        try
        {
            if (wasFlushed)
                holder.flush();
            holder.apply( map );
        }
        finally {
            wasFlushed = false;
            map.clear();
        }
    }
}

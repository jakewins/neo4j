package org.neo4j.kernel.impl.api;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.helpers.Function;

public class KernelSchemaStateHolder implements UpdateableSchemaStateHolder
{
    private Map<Object, Object> state = new HashMap<Object, Object>();

    public <K, V> V getOrCreate( K key, Class<V> clazz, Function<K, V> creator ) {
        return getOrCreateAndPut( key, clazz, creator, state );
    }

    public <K, V> V getOrCreateAndPut( K key, Class<V> clazz, Function<K, V> creator, Map<Object, Object> targetMap )
    {
        if ( state.containsKey( key ) )
        {
            return clazz.cast( state.get( key ) );
        }

        V value = creator.apply( key );
        targetMap.put( key, value );

        return value;
    }

    public <K, V> void apply( Map<K, V> updates )
    {
        for (Map.Entry<K, V> update : updates.entrySet())
        {
            final K key = update.getKey();
            if ( state.containsKey( key ) )
            {
                throw new IllegalArgumentException(
                          "Cannot update key: " + key.toString()
                        + ". Already existing in schema state" );
            }
        }
        state.putAll( updates );
    }

    public void flush()
    {
        state.clear();
    }

    @Override
    public void commit()
    {
        // already committed since operations on KernelSchemaStateHolder are instantaneous
    }
}

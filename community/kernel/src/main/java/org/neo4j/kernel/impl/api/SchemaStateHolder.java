package org.neo4j.kernel.impl.api;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.helpers.Function;

public class SchemaStateHolder
{
    private Map<Object, Object> state = new HashMap<Object, Object>();

    public <T> T getOrCreateFromSchemaState( Object key, Function<Object, T> creator )
    {
        if(state.containsKey( key ))
        {
            return (T) state.get( key );
        }

        T value = creator.apply( key );
        state.put( key, value );

        return value;
    }

    public void flush()
    {

    }
}

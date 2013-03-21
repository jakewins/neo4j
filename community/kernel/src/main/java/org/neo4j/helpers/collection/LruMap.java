package org.neo4j.helpers.collection;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruMap<K, V> extends LinkedHashMap<K,V >
{
    private final int maxEntries;

    public LruMap( int maxEntries )
    {
        super( maxEntries, 1.0f, true );
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        return super.size() > maxEntries;
    }
}

package org.neo4j.kernel.impl.api;

import org.neo4j.helpers.Function;

public interface SchemaState {

    <K, V>V get( K key );

    <K, V> V getOrCreate( K key, Function<K, V> creator );

    void flush();
}

/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.kernel.impl.api;

import java.util.Map;

import org.neo4j.helpers.Function;

public interface SchemaStateHolder
{
    <K, V> V getOrCreate( K key, Class<V> clazz, Function<K, V> creator );

    <K, V> V getOrCreateAndPut( K key, Class<V> clazz, Function<K, V> creator, Map<Object, Object> targetMap );

    void flush();

    void commit();
}

/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.kernel.impl.api;

import org.neo4j.helpers.Function;

public interface SchemaStateHolder
{
    public <K, V> V getOrCreate( K key, Class<V> clazz, Function<K, V> creator );

    public void flush();

    public void commit();
}

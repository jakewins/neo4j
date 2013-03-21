/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.kernel.impl.api;

import java.util.Map;

public interface SchemaStateStore
{
    <K, V>V get( K key );

    <K, V> void apply( Map<K, V> updates );

    void flush();
}

/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.kernel.impl.api;

import org.neo4j.helpers.Function;

public interface TransactionalSchemaState
{
    <K, V> V getOrCreate( K key, Function<K, V> creator );

    void flush();

    void commit();
}

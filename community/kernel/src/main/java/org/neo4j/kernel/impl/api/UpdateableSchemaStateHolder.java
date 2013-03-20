/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.kernel.impl.api;

import java.util.Map;

public interface UpdateableSchemaStateHolder extends SchemaStateHolder
{
    <K, V> void apply(Map<K, V> updates);
}

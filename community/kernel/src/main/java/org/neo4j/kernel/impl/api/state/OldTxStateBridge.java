/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.kernel.impl.api.state;

import org.neo4j.kernel.impl.api.DiffSets;

/**
 * Temporary anti-corruption while the old {@link org.neo4j.kernel.impl.core.TransactionState} class
 * still remains.
 */
public interface OldTxStateBridge
{

    Iterable<Long> getDeletedNodes();

    /**
     * A diff set of nodes that have had the given property key and value added or removed/changed.
     */
    DiffSets<Long> getNodesWithChangedProperty(long propertyKey, Object value);
}

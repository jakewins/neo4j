/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.kernel.impl.api.state;

/**
 * Temporary anti-corruption while the old {@link org.neo4j.kernel.impl.core.TransactionState} class
 * still remains.
 */
public interface OldTxStateBridge
{
    Iterable<Long> getDeletedNodes();
}

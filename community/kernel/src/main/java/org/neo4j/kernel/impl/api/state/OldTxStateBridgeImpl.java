package org.neo4j.kernel.impl.api.state;

import org.neo4j.kernel.impl.core.TransactionState;

public class OldTxStateBridgeImpl implements OldTxStateBridge
{

    private final TransactionState state;

    public OldTxStateBridgeImpl(TransactionState transactionState)
    {
        this.state = transactionState;
    }

    @Override
    public Iterable<Long> getDeletedNodes()
    {
        return state.getDeletedNodes();
    }
}

package org.neo4j.kernel.impl.api.state;

import org.neo4j.kernel.impl.api.DiffSets;
import org.neo4j.kernel.impl.core.TransactionState;
import org.neo4j.kernel.impl.core.WritableTransactionState;
import org.neo4j.kernel.impl.nioneo.store.PropertyData;
import org.neo4j.kernel.impl.util.ArrayMap;

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

    @Override
    public DiffSets<Long> getNodesWithChangedProperty( long propertyKey, Object value )
    {
        DiffSets<Long> diff = new DiffSets<Long>();
        Iterable<WritableTransactionState.CowNodeElement> changedNodes = state.getChangedNodes();

        for ( WritableTransactionState.CowNodeElement changedNode : changedNodes )
        {

            // All nodes where the property has been removed altogether
            ArrayMap<Integer,PropertyData> propRmMap = changedNode.getPropertyRemoveMap( false );
            if(propRmMap != null)
            {
                for ( PropertyData propertyData : propRmMap.values() )
                {
                    if( propertyData.getIndex() == propertyKey && propertyData.getValue().equals( value ) )
                    {
                        diff.remove( changedNode.getId() );
                    }
                }
            }

            // All nodes where property has been added or changed
            ArrayMap<Integer,PropertyData> propAddMap = changedNode.getPropertyAddMap( false );
            if(propAddMap != null)
            {
                for ( PropertyData propertyData : propAddMap.values() )
                {
                    if( propertyData.getIndex() == propertyKey )
                    {
                        // Added if value is the same, removed if value is different.
                        if( propertyData.getValue().equals( value ))
                        {
                            diff.add( changedNode.getId() );
                        } else
                        {
                            diff.remove( changedNode.getId() );
                        }
                    }
                }
            }
        }


        return diff;
    }
}

package org.neo4j.kernel.impl.api.state;

import static junit.framework.Assert.assertEquals;
import static org.neo4j.helpers.collection.IteratorUtil.asSet;

import org.junit.Test;
import org.neo4j.kernel.impl.core.TransactionState;
import org.neo4j.kernel.impl.core.WritableTransactionState;
import org.neo4j.kernel.logging.DevNullLoggingService;

public class OldTxStateBridgeImplTest
{

    @Test
    public void shouldListDeletedNodes() throws Exception
    {
        // Given
        TransactionState state = new WritableTransactionState( null, null, new DevNullLoggingService(), null, null, null );
        OldTxStateBridge bridge = new OldTxStateBridgeImpl( state );

        state.deleteNode( 1l );

        // When
        Iterable<Long> deletedNodes = bridge.getDeletedNodes();

        // Then
        assertEquals(asSet(1l), asSet(deletedNodes));
    }

}

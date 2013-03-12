package org.neo4j.kernel.impl.api.state;

import static junit.framework.Assert.assertEquals;
import static org.neo4j.helpers.collection.IteratorUtil.asSet;

import org.junit.Test;
import org.neo4j.kernel.impl.api.DiffSets;
import org.neo4j.kernel.impl.core.NodeImpl;
import org.neo4j.kernel.impl.core.TransactionState;
import org.neo4j.kernel.impl.core.WritableTransactionState;
import org.neo4j.kernel.impl.nioneo.store.PropertyDatas;
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

    @Test
    public void shouldListNodesWithPropertyAdded() throws Exception
    {
        // Given
        long nodeId = 1l;
        int propertyKey = 2;
        int value = 1337;

        WritableTransactionState state = new WritableTransactionState( null, null, new DevNullLoggingService(), null, null, null );
        OldTxStateBridge bridge = new OldTxStateBridgeImpl( state );

        NodeImpl node = new NodeImpl( nodeId, -1, -1);

        // And Given that I've added a relevant property
        state.getOrCreateCowPropertyAddMap( node ).put( 2, PropertyDatas.forInt( propertyKey, -1l, value ) );

        // When
        DiffSets<Long> nodes = bridge.getNodesWithChangedProperty( propertyKey, value );

        // Then
        assertEquals(asSet(nodeId), nodes.getAdded());
        assertEquals(asSet(),       nodes.getRemoved());
    }

    @Test
    public void shouldListNodesWithPropertyRemoved() throws Exception
    {
        // Given
        long nodeId = 1l;
        int propertyKey = 2;
        int value = 1337;

        WritableTransactionState state = new WritableTransactionState( null, null, new DevNullLoggingService(), null, null, null );
        OldTxStateBridge bridge = new OldTxStateBridgeImpl( state );

        NodeImpl node = new NodeImpl( nodeId, -1, -1);

        // And Given that I've added a relevant property
        state.getOrCreateCowPropertyRemoveMap( node ).put( 2, PropertyDatas.forInt( propertyKey, -1l, value ) );

        // When
        DiffSets<Long> nodes = bridge.getNodesWithChangedProperty( propertyKey, value );

        // Then
        assertEquals(asSet(),       nodes.getAdded());
        assertEquals(asSet(nodeId), nodes.getRemoved());
    }

    @Test
    public void shouldListNodesWithPropertyChanged() throws Exception
    {
        // Given
        long nodeId = 1l;
        int propertyKey = 2;
        int value = 1337;

        WritableTransactionState state = new WritableTransactionState( null, null, new DevNullLoggingService(), null, null, null );
        OldTxStateBridge bridge = new OldTxStateBridgeImpl( state );

        NodeImpl node = new NodeImpl( nodeId, -1, -1);

        // And Given that I've added a relevant property
        state.getOrCreateCowPropertyAddMap( node ).put( 2, PropertyDatas.forInt( propertyKey, -1l, /*other value*/7331));

        // When
        DiffSets<Long> nodes = bridge.getNodesWithChangedProperty( propertyKey, value );

        // Then
        assertEquals(asSet(),       nodes.getAdded());
        assertEquals(asSet(nodeId), nodes.getRemoved());
    }

}

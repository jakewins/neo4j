package org.neo4j.kernel.impl.api.store;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * Test reading committed node and relationships from disk.
 */
public class DiskLayerNodeAndRelTest extends DiskLayerTest
{
    @Test
    public void shouldTellIfNodeExists() throws Exception
    {
        // Given
        long created = createLabeledNode( db, map() ).getId();
        long createdAndRemoved = createLabeledNode( db, map() ).getId();
        long neverExisted = createdAndRemoved + 99;

        try( Transaction tx = db.beginTx() )
        {
            db.getNodeById( createdAndRemoved ).delete();
            tx.success();
        }

        // When & then
        assertTrue(  disk.nodeExists( created ));
        assertFalse( disk.nodeExists( createdAndRemoved ) );
        assertFalse( disk.nodeExists( neverExisted ) );
    }

    @Test
    public void shouldTellIfRelExists() throws Exception
    {
        // Given
        long node = createLabeledNode( db, map() ).getId();
        long created, createdAndRemoved, neverExisted;

        try( Transaction tx = db.beginTx() )
        {
            created = db.createNode().createRelationshipTo( db.createNode(), withName( "Banana" ) ).getId();
            createdAndRemoved = db.createNode().createRelationshipTo( db.createNode(), withName( "Banana" ) ).getId();
            tx.success();
        }

        try( Transaction tx = db.beginTx() )
        {
            db.getRelationshipById( createdAndRemoved ).delete();
            tx.success();
        }

        neverExisted = created + 99;

        // When & then
        assertTrue(  disk.relationshipExists( node ));
        assertFalse( disk.relationshipExists( createdAndRemoved ) );
        assertFalse( disk.relationshipExists( neverExisted ) );
    }

}

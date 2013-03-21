/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.neo4j.graphdb.DynamicLabel.label;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.cache_type;
import static org.neo4j.helpers.collection.IteratorUtil.addToCollection;
import static org.neo4j.helpers.collection.IteratorUtil.asSet;
import static org.neo4j.helpers.collection.IteratorUtil.asUniqueSet;
import static org.neo4j.helpers.collection.MapUtil.map;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.api.PropertyKeyNotFoundException;
import org.neo4j.kernel.api.PropertyNotFoundException;
import org.neo4j.kernel.api.StatementContext;
import org.neo4j.kernel.impl.api.index.IndexDescriptor;
import org.neo4j.kernel.impl.api.index.IndexingService;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.core.PropertyIndexManager;
import org.neo4j.kernel.impl.nioneo.store.NeoStore;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreXaDataSource;
import org.neo4j.kernel.impl.persistence.PersistenceManager;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.TestGraphDatabaseFactory;

public class StoreStatementContextTest
{
    @Test
    public void should_be_able_to_read_a_node_property() throws Exception
    {
        // GIVEN
        String propertyKey = "myproperty";
        int propertyValue = 42;

        Transaction tx = db.beginTx();
        Node node = db.createNode();
        long nodeId = node.getId();
        node.setProperty( propertyKey, propertyValue );
        tx.success();
        tx.finish();

        // WHEN
        long propertyKeyId = statement.getPropertyKeyId( propertyKey );
        int result = (Integer) statement.getNodePropertyValue( nodeId, propertyKeyId );

        // THEN
        assertThat( propertyValue, equalTo( result ) );
    }

    @Test
    public void should_throw_when_reading_a_missing_node_property() throws Exception
    {
        // GIVEN
        String propertyKey = "myproperty";
        int propertyValue = 42;

        Transaction tx = db.beginTx();
        Node node = db.createNode();
        node.setProperty( propertyKey, propertyValue );
        long nodeId = db.createNode().getId();
        tx.success();
        tx.finish();

        // WHEN
        long propertyKeyId = statement.getPropertyKeyId( propertyKey );
        try
        {
            statement.getNodePropertyValue( nodeId, propertyKeyId );

            fail( "Should have thrown exception" );
        }
        // THEN
        catch ( PropertyNotFoundException e )
        {
            assertEquals( "No property with id 0 on node with id 2", e.getMessage() );
        }
    }

    @Test
    public void should_be_able_to_add_label_to_node() throws Exception
    {
        // GIVEN
        Transaction tx = db.beginTx();
        long nodeId = db.createNode().getId();
        String labelName = label.name();
        long labelId = statement.getOrCreateLabelId( labelName );

        // WHEN
        statement.addLabelToNode( labelId, nodeId );
        tx.success();
        tx.finish();

        // THEN
        assertTrue( "Label " + labelName + " wasn't set on " + nodeId, statement.isLabelSetOnNode( labelId, nodeId ) );
    }
    
    @Test
    public void should_be_able_to_list_labels_for_node() throws Exception
    {
        // GIVEN
        Transaction tx = db.beginTx();
        long nodeId = db.createNode().getId();
        String labelName1 = label.name(), labelName2 = label2.name();
        long labelId1 = statement.getOrCreateLabelId( labelName1 );
        long labelId2 = statement.getOrCreateLabelId( labelName2 );
        statement.addLabelToNode( labelId1, nodeId );
        statement.addLabelToNode( labelId2, nodeId );
        tx.success();
        tx.finish();

        // THEN
        Iterable<Long> readLabels = statement.getLabelsForNode( nodeId );
        assertEquals( new HashSet<Long>( asList( labelId1, labelId2 ) ),
                addToCollection( readLabels, new HashSet<Long>() ) );
    }
    
    @Test
    public void should_be_able_to_get_label_name_for_label() throws Exception
    {
        // GIVEN
        String labelName = label.name();
        long labelId = statement.getOrCreateLabelId( labelName );

        // WHEN
        String readLabelName = statement.getLabelName( labelId );

        // THEN
        assertEquals( labelName, readLabelName );
    }

    @Test
    public void should_be_able_to_remove_node_label() throws Exception
    {
        // GIVEN
        Transaction tx = db.beginTx();
        String labelName = label.name();
        long labelId = statement.getOrCreateLabelId( labelName );
        long node = db.createNode().getId();
        statement.addLabelToNode( labelId, node );
        tx.success();
        tx.finish();

        // WHEN
        tx = db.beginTx();
        statement.removeLabelFromNode( labelId, node );
        tx.success();
        tx.finish();

        // THEN
        assertFalse( statement.isLabelSetOnNode( labelId, node ) );
    }

    /*
     * This test doesn't really belong here, but OTOH it does, as it has to do with this specific
     * store solution. It creates its own IGD with cache_type:none to try reproduce to trigger the problem.
     */
    @Test
    public void labels_should_not_leak_out_as_properties() throws Exception
    {
        // GIVEN
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
                .setConfig( cache_type, "none" ).newGraphDatabase();
        Node node = createLabeledNode( db, map( "name", "Node" ), label );

        // WHEN
        Iterable<String> propertyKeys = node.getPropertyKeys();
        
        // THEN
        assertEquals( asSet( "name" ), asSet( propertyKeys ) );
    }
    
    @Test
    public void should_return_true_when_adding_new_label() throws Exception
    {
        // GIVEN
        Node node = createLabeledNode( db, map() );

        // WHEN
        Transaction tx = db.beginTx();
        boolean added = false;
        try
        {
            long labelId = statement.getOrCreateLabelId( label.name() );
            added = statement.addLabelToNode( labelId, node.getId() );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        // THEN
        assertTrue( "Label should have been added", added );
    }
    
    @Test
    public void should_return_false_when_adding_existing_label() throws Exception
    {
        // GIVEN
        Node node = createLabeledNode( db, map(), label );

        // WHEN
        Transaction tx = db.beginTx();
        boolean added = false;
        try
        {
            long labelId = statement.getOrCreateLabelId( label.name() );
            added = statement.addLabelToNode( labelId, node.getId() );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        // THEN
        assertFalse( "Label should not have been added", added );
    }
    
    @Test
    public void should_return_true_when_remove_existing_label() throws Exception
    {
        // GIVEN
        Node node = createLabeledNode( db, map(), label );

        // WHEN
        Transaction tx = db.beginTx();
        boolean removed = false;
        try
        {
            long labelId = statement.getOrCreateLabelId( label.name() );
            removed = statement.removeLabelFromNode( labelId, node.getId() );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        // THEN
        assertTrue( "Label should have been removed", removed );
    }
    
    @Test
    public void should_return_false_when_removing_non_existent_label() throws Exception
    {
        // GIVEN
        Node node = createLabeledNode( db, map() );

        // WHEN
        Transaction tx = db.beginTx();
        boolean removed = false;
        try
        {
            long labelId = statement.getOrCreateLabelId( label.name() );
            removed = statement.addLabelToNode( labelId, node.getId() );
            tx.success();
        }
        finally
        {
            tx.finish();
        }

        // THEN
        assertTrue( "Label should not have been removed", removed );
    }
    
    @Test
    public void should_return_all_nodes_with_label() throws Exception
    {
        // GIVEN
        Node node1 = createLabeledNode( db, map( "name", "First", "age", 1L ), label );
        Node node2 = createLabeledNode( db, map( "type", "Node", "count", 10 ), label, label2 );

        // WHEN
        Iterable<Long> nodesForLabel1 = statement.getNodesWithLabel( statement.getLabelId( label.name() ) );
        Iterable<Long> nodesForLabel2 = statement.getNodesWithLabel( statement.getLabelId( label2.name() ) );

        // THEN
        assertEquals( asSet( node1.getId(), node2.getId() ), asSet( nodesForLabel1 ) );
        assertEquals( asSet( node2.getId() ), asSet( nodesForLabel2 ) );
    }
    
    @Test
    public void should_create_property_key_if_not_exists() throws Exception
    {
        // WHEN
        long id = statement.getOrCreatePropertyKeyId( propertyKey );

        // THEN
        assertTrue( "Should have created a non-negative id", id >= 0 );
    }
    
    @Test
    public void should_get_previously_created_property_key() throws Exception
    {
        // GIVEN
        long id = statement.getOrCreatePropertyKeyId( propertyKey );

        // WHEN
        long secondId = statement.getPropertyKeyId( propertyKey );

        // THEN
        assertEquals( id, secondId );
    }
    
    @Test
    public void should_be_able_to_get_or_create_previously_created_property_key() throws Exception
    {
        // GIVEN
        long id = statement.getOrCreatePropertyKeyId( propertyKey );

        // WHEN
        long secondId = statement.getOrCreatePropertyKeyId( propertyKey );

        // THEN
        assertEquals( id, secondId );
    }
    
    @Test
    public void should_fail_if_get_non_existent_property_key() throws Exception
    {
        // WHEN
        try
        {
            statement.getPropertyKeyId( "non-existent-property-key" );
            fail( "Should have failed with property key not found exception" );
        }
        catch ( PropertyKeyNotFoundException e )
        {
            // Good
        }
    }

    @Test
    public void should_get_index_descriptor_by_id() throws Exception
    {
        // GIVEN
        IndexDescriptor idxDesc = new IndexDescriptor(1, 2);

        IndexingService mockIndexService = mock(IndexingService.class);
        when( mockIndexService.getIndexDescriptor( 1337l ) ).thenReturn( idxDesc );

        StoreStatementContext ctx = new StoreStatementContext( null, null, null, mock( NeoStore.class ),
                mockIndexService, null );

        // WHEN
        IndexDescriptor idx = ctx.getIndexDescriptor( 1337l );

        // THEN
        assertEquals( idx, idxDesc );
    }
    
    @Test
    public void should_find_nodes_with_given_label_and_property_via_index() throws Exception
    {
        // GIVEN
        long indexId = createIndexAndAwaitOnline( label, propertyKey );
        String name = "Mr. Taylor";
        Node mrTaylor = createLabeledNode( db, map( propertyKey, name ), label );

        // WHEN
        Set<Long> foundNodes = asUniqueSet( statement.exactIndexLookup( indexId, name ) );

        // THEN
        assertEquals( asSet( mrTaylor.getId() ), foundNodes );
    }
    
    private GraphDatabaseAPI db;
    private StatementContext statement;
    private final Label label = label( "first-label" ), label2 = label( "second-label" );
    private final String propertyKey = "name";

    @Before
    public void before()
    {
        db = new ImpermanentGraphDatabase();
        IndexingService indexingService = db.getDependencyResolver().resolveDependency( IndexingService.class );
        @SuppressWarnings("deprecation")// Ooh, jucky
        NeoStoreXaDataSource neoStoreDataSource = db.getDependencyResolver()
                .resolveDependency( XaDataSourceManager.class ).getNeoStoreDataSource();
        statement = new StoreStatementContext(
                db.getDependencyResolver().resolveDependency( PropertyIndexManager.class ),
                db.getDependencyResolver().resolveDependency( PersistenceManager.class ),
                db.getDependencyResolver().resolveDependency( NodeManager.class ),
                neoStoreDataSource.getNeoStore(),
                indexingService, new IndexReaderFactory.Caching( indexingService ));
    }

    @After
    public void after()
    {
        db.shutdown();
    }

    private static Node createLabeledNode( GraphDatabaseService db, Map<String, Object> properties, Label... labels )
    {
        Transaction tx = db.beginTx();
        try
        {
            Node node = db.createNode( labels );
            for ( Map.Entry<String, Object> property : properties.entrySet() )
                node.setProperty( property.getKey(), property.getValue() );
            tx.success();
            return node;
        }
        finally
        {
            tx.finish();
        }
    }

    private long createIndexAndAwaitOnline( Label label, String propertyKey ) throws Exception
    {
        Transaction tx = db.beginTx();
        IndexDefinition index = null;
        try
        {
            index = db.schema().indexCreator( label ).on( propertyKey ).create();
            tx.success();
        }
        finally
        {
            tx.finish();
        }
        
        db.schema().awaitIndexOnline( index, 10, SECONDS );
        return statement.getIndexRule( statement.getLabelId( label.name() ),
                statement.getPropertyKeyId( propertyKey ) ).getId();
    }
}

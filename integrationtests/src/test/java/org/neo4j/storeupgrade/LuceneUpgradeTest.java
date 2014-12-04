/**
 * Copyright (c) 2002-2014 "Neo Technology,"
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
package org.neo4j.storeupgrade;

import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.tooling.GlobalGraphOperations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.neo4j.graphdb.DynamicLabel.label;
import static org.neo4j.helpers.collection.IteratorUtil.count;

public class LuceneUpgradeTest
{
    @Test
    public void asd() throws Exception
    {
        // Given
        GraphDatabaseService db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder( "/Users/jake/Downloads/neo4j-enterprise-2.2.0-M01/data/graph.db" )
                .setConfig( GraphDatabaseSettings.allow_store_upgrade, "true" )
                .newGraphDatabase();
        try
        {
            try(Transaction tx = db.beginTx())
            {
                assertAllIndexesOnline( db );
                for ( Node node : GlobalGraphOperations.at( db ).getAllNodes() )
                {
                    System.out.println(node + " - " + Iterables.toList(node.getLabels()));
                    for ( String s : node.getPropertyKeys() )
                    {
                        System.out.println(s + ": " + node.getProperty( s ));
                    }

                }

                assertThat( count( db.findNodes( label( "Indexed" ) ) ), equalTo( 6 ) );

                assertThat( db.findNode( label( "Indexed" ), "schemaindexed", 12345 ).getProperty( "schemaindexed" ),
                        equalTo( (Object) 12345 ) );

                assertThat(db.findNode( label("Indexed"), "schemaindexed", 1.2345 ).getProperty( "schemaindexed" ),
                        equalTo( (Object)1.2345 ));

                assertThat(db.findNode( label("Indexed"), "schemaindexed",
                                new String[]{"1","2","3","4","5"} ).getProperty( "schemaindexed" ),
                        equalTo( (Object)new String[]{"1","2","3","4","5"}  ));

                assertThat(db.findNode( label("Indexed"), "schemaindexed", "12345" ).getProperty( "schemaindexed" ),
                        equalTo( (Object)"12345" ));

                assertThat(db.findNode( label("Indexed"), "schemaindexed", true ).getProperty( "schemaindexed" ),
                        equalTo( (Object)true ));

                assertThat(db.findNode( label("Indexed"), "schemaindexed", false ).getProperty( "schemaindexed" ),
                        equalTo( (Object)false ));

            }
        }
        finally
        {
            db.shutdown();
        }
    }

    private void assertAllIndexesOnline( GraphDatabaseService db )
    {
        for ( IndexDefinition indexDefinition : db.schema().getIndexes() )
        {
            assertThat(db.schema().getIndexState( indexDefinition ), equalTo( Schema.IndexState.ONLINE));
        }
    }
}

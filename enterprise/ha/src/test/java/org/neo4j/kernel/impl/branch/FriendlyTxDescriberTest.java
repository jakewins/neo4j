/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.branch;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.test.EphemeralFileSystemRule;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.neo4j.graphdb.DynamicLabel.label;

public class FriendlyTxDescriberTest
{
    @Rule
    public EphemeralFileSystemRule fsRule = new EphemeralFileSystemRule();
    private GraphDatabaseAPI db;

    @Before
    public void setup()
    {
        db = (GraphDatabaseAPI) new TestGraphDatabaseFactory().setFileSystem( fsRule.get() ).newImpermanentDatabase( "db" );
    }

    @After
    public void shutdown()
    {
        db.shutdown();
    }

    @Test
    public void shouldDescribeNodeCreateWithLabelAndProperties() throws Exception
    {
        // Given
        try( Transaction tx = db.beginTx() )
        {
            Node node = db.createNode( label( "User" ), label( "Admin" ));
            node.setProperty( "short_str", "hello" );
//            node.setProperty( "long_str", "hellooiujasdfaiuflaisuhfliaublaublaiwurhglaieubrglarbglaiwurbflawrubfalwrub" );
//            node.setProperty( "bytes", new byte[]{1,2,3,4,5,6,7,8,9,1,2,2,2,2,2,2,2,2,2,2,2,2,2} );
            tx.success();
        }

        // When & then
        String actual = descriptionOfLatestTx();
        System.out.println(actual);
        assertThat( actual, equalTo(""));

        // Then
    }

    private String descriptionOfLatestTx() throws IOException
    {
        db.getDependencyResolver().resolveDependency( XaDataSourceManager.class ).rotateLogicalLogs();
        return new FriendlyTxDescriber(db).describe(latestTx());
    }

    private Tx latestTx() throws IOException
    {
        try( ResourceIterator<Tx> txs = new TransactionLogReader( fsRule.get() ).readLogs( new File("db"), TransactionLogReader.Direction.BACKWARD ))
        {
            return txs.next();
        }
    }
}

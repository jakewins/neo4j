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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.DefaultFileSystemAbstraction;
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction;

import static org.neo4j.kernel.impl.branch.TransactionLogReader.Direction.BACKWARD;

public class BranchAnalysis
{

    private final File main;
    private final File branch;
    private final TransactionLogReader logReader;

    public BranchAnalysis( File main, File branch, FileSystemAbstraction fileSystem )
    {
        this.main = main;
        this.branch = branch;
        this.logReader = new TransactionLogReader( fileSystem );
    }

    public static void main(String ... args) throws IOException
    {
        File graph = new File("/Users/jake/Downloads/scripps/182/snikzlp182/main");
        // /Users/jake/Downloads/scripps/newbranch/branched/1407866600648
        // /Users/jake/Downloads/scripps/newbranch/branched/1409562191491
        File branch = new File("/Users/jake/Downloads/scripps/182/neo4j-enterprise/data/graph.db/branched/1410371001189");

        FileSystemAbstraction fileSystem = new DefaultFileSystemAbstraction();

        new BranchAnalysis(graph, branch, fileSystem).analyze();
    }

    public void analyze() throws IOException
    {
        // 1. Find the first branching transaction
//        long branchTxId = findBranchPoint();
//        if(branchTxId == -1) return;

        // 2. Go over the branched entries
//        System.out.println("Database branched at: " + branchTxId);
        File db1 = new File("/Users/jake/Downloads/fiftythree/graph.db");
        ResourceIterator<Tx> branchedEntries = logReader.readLogs( db1, -1, BACKWARD );

        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( db1.getAbsolutePath() );
        try
        {
            FriendlyTxDescriber describer = new FriendlyTxDescriber( (org.neo4j.kernel.GraphDatabaseAPI) db );
            while ( branchedEntries.hasNext() )
            {
                Tx next = branchedEntries.next();
                next.dump( System.out );
                System.out.println( describer.describe( next ) );

                break;
            }
        }
        finally
        {
            db.shutdown();
        }
    }

    private long findBranchPoint() throws IOException
    {
        try(ResourceIterator<Tx> branchedEntries = logReader.readLogs( branch, BACKWARD ))
        {

            if ( !branchedEntries.hasNext() )
            {
                System.out.println( "Error: No transactions found in [" + branch.getAbsolutePath() + "]" );
                return -1;
            }

            Tx branchedTx = branchedEntries.next(), prevBranchedEntry = null;

            try(ResourceIterator<Tx> mainEntries = logReader.readLogs( main, branchedTx.id(), BACKWARD ))
            {
                if ( !mainEntries.hasNext() )
                {
                    System.out.println( "Error: The first branched tx is " + branchedTx.id() + ", but the main db transaction log does not go that far back." );
                    return -1;
                }

                Tx mainEntry = mainEntries.next();

                while ( branchedEntries.hasNext() )
                {
                    while ( mainEntry.id() > branchedTx.id() )
                    {
                        if ( !mainEntries.hasNext() )
                        {
                            System.out.println( "Error: Reached end of mainline transactions without finding the point of branching. Likely the branch is too old (and mainline transaction logs have been pruned too far)." );
                            return -1;
                        }
                        mainEntry = mainEntries.next();
                    }

                    if ( mainEntry.checksum() == branchedTx.checksum() && mainEntry.id() == branchedTx.id() )
                    {
                        if ( prevBranchedEntry == null )
                        {
                            System.out.println( "There is no branching point, all transactions in the 'branched' db are contained in the mainline database as well." );
                            return -1;
                        }
                        return prevBranchedEntry.id();
                    }


                    prevBranchedEntry = branchedTx;
                    branchedTx = branchedEntries.next();
                }
            }

            System.out.println( "Error: Reached end of branched transaction logs without finding branch point." );
            return -1;
        }
    }


}

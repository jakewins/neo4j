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
package org.neo4j;

import org.HdrHistogram.Histogram;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Random;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.store.NeoStore;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class Asd
{
    private static final long GIGABYTE = 1024 * 1024 * 1024l;

    private static final String name = "nostoresync";
    private static final Label label = DynamicLabel.label( "OnOff" );

    public static void main(String ... args) throws IOException
    {
        // Goal: Attempt to expose cost of store flushing during log rotation.
        // Use case:
        //  - Create a base store that takes up like a gigabyte of RAM
        //  - Run update load where the transactions themselves are small, but touch a wide span of mapped pages
        //    Specifically, the load should pick a random node, and toggle a label on it.
        //  - Measure latencies for this, *importantly* considering CO (because that will be vital here)

        GraphDatabaseService db = init();
        try
        {
            int highestNodeId = highestNodeId( db );

            Histogram histogram = new Histogram( 3600000000000L, 3 );
            System.out.println("[Load] Running");
            runLoad( histogram, db, highestNodeId, 5 * 60 );
            storeResults( histogram );
        }
        finally
        {
            db.shutdown();
        }
    }


    private static void runLoad( Histogram histogram, GraphDatabaseService db, int highestNodeId, int runtimeInSeconds )
    {
        long deadline = System.currentTimeMillis() + (1000 * runtimeInSeconds);
        Random rand = new Random();

        while(System.currentTimeMillis() < deadline)
        {
            long start = System.nanoTime();
            try( Transaction tx = db.beginTx() )
            {
                for ( int i = 0; i < 5; i++ )
                {
                    Node node = db.getNodeById( rand.nextInt( highestNodeId ) );
                    if(node.hasLabel( label ))
                    {
                        node.removeLabel( label );
                    }
                    else
                    {
                        node.addLabel( label );
                    }
                }
                tx.success();
            }
            histogram.recordValue( System.nanoTime() - start );
        }
    }

    private static int highestNodeId( GraphDatabaseService db )
    {
        return (int) ((GraphDatabaseAPI)db).getDependencyResolver().resolveDependency( NeoStore.class )
                    .getNodeStore().getHighId();
    }

    private static GraphDatabaseService init()
    {
        boolean dbExisted = new File("/tmp/db").exists();

        if(!dbExisted)
        {
            createALotOfNodes( "/tmp/db", 1 * GIGABYTE );
        }
        else
        {
            System.out.println("[Init] Db existed, assuming it was populated, skipping data generation.");
        }
        return new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( "/tmp/db" )
                .setConfig( GraphDatabaseSettings.logical_log_rotation_threshold, "1M" ).newGraphDatabase();
    }

    private static long createALotOfNodes( String path, long sizeInBytes )
    {
        System.out.println("[Init] Creating dataset");
        BatchInserter inserter = BatchInserters.inserter( path );
        long numNodes = sizeInBytes / 15;
        long nodesLeft = numNodes;
        Map<String,Object> map = MapUtil.map();
        while(nodesLeft > 0)
        {
            inserter.createNode( map );
            nodesLeft--;
            if(nodesLeft % 100_000 == 0)
            {
                System.out.println("[Init] " + nodesLeft );
            }
        }
        inserter.shutdown();
        System.out.println("[Init] Done");
        return numNodes;
    }

    private static void storeResults( Histogram histogram ) throws IOException
    {
        File outFile = new File(String.format( "%s-%s.histogram", name, System.currentTimeMillis() ));
        try(FileOutputStream fso = new FileOutputStream( outFile );
            PrintStream ps = new PrintStream( fso ))
        {
            System.out.println( "Recorded latencies [in usec] for single message request/response" );
            histogram.getHistogramData().outputPercentileDistribution( System.out, 1000.0 );

            System.out.println("Stored output in: " + outFile.getAbsolutePath());
            histogram.getHistogramData().outputPercentileDistribution( ps, 1000.0 );
        }
    }
}

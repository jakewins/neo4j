package org.neo4j.bolt.v1.transport.integration;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.neo4j.bolt.BoltKernelExtension;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import static java.util.Arrays.asList;
import static org.neo4j.bolt.BoltKernelExtension.Settings.connector;
import static org.neo4j.helpers.collection.MapUtil.map;

public class AllocationIT
{
    private static AtomicBoolean sample = new AtomicBoolean( false );

    public static void main(String ... args) throws IOException
    {
        // Given
        final GraphDatabaseService gdb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder("/Users/jake/Downloads/social-network-10K.neo4j-2.3-SNAPSHOT/graph.db")
                .setConfig( GraphDatabaseSettings.allow_store_upgrade, "true" )
                .setConfig( connector( 0, BoltKernelExtension.Settings.enabled ), "true" )
                .newGraphDatabase();

        AllocationRepository repo = new AllocationRepository();


        // Warmup-ish, really mainly making sure we've loaded the pertinent classes etc.
        // I have no idea how the instrumentation here plays with stuff like inlining and escape analysis..
        for ( int i = 0; i < 10; i++ )
        {
            runScenario( gdb );
        }

        Sampler sampler = ( count, desc, newObj, size ) -> {
            if( !sample.get() || blackList.contains( desc ) ) {
                return;
            }
            String trace = stackTrace();
            if( trace != null )
            {
                if(count != -1) {
                    desc += "[]";
                }
                repo.record( desc + " - " + trace, size );
            }
        };
        AllocationRecorder.addSampler( sampler );

        // When
        System.out.println("Running test");
        sample.set( true );
        runScenario( gdb );
        sample.set( false );

        // Then

        AllocationRecorder.removeSampler( sampler );

        gdb.shutdown();

        repo.describeTo(System.out);
    }

    private static Random random = new Random();
    private static Result.ResultVisitor<RuntimeException> voidVisitor = row -> true;

    private static void runScenario( GraphDatabaseService gdb ) throws IOException
    {
        waitForBoltRun();

//        runStatement( gdb );
    }

    private static void runStatement( GraphDatabaseService gdb )
    {
        Map<String,Object> userId = map( "userId", random.nextInt( 10_000 ) );
        System.out.println(userId);
        Result result = gdb.execute( "MATCH (user:User { userId: {userId} } ) -[:FRIEND]- () -[:FRIEND]- (distantFriend) " +
                                     "RETURN COUNT(distinct distantFriend)", userId );
        result.accept( voidVisitor );
    }

    private static void waitForBoltRun() throws IOException
    {
        System.out.println("Please run a query now, then press any key.. ");
        System.in.read();
    }

    private static Set<String> blackList = new HashSet<>( asList( "java/util/ArrayList$Itr" ) );
    private static Set<String> utils = new HashSet<>( asList(
            "org.neo4j.collection.primitive",
            "org.neo4j.bolt.v1.transport.integration",
            "org.neo4j.cypher.internal.compiler.v3_0.pipes.MutableMaps$") );

    private static String stackTrace()
    {
        outer: for ( StackTraceElement element : Thread.currentThread().getStackTrace() )
        {
            String el = element.toString();
            if( el.contains( "org.neo4j" ) ) {
                for ( String util : utils )
                {
                    if( el.contains( util ))
                    {
                        continue outer;
                    }
                }

                return el;
            }
        }
        return null;
    }
}

class AllocationRepository
{
    private final Map<String, ClassStats> repo = new HashMap<>();

    public synchronized void record( String name, long size )
    {
        ClassStats stats = repo.get( name );
        if( stats == null )
        {
            stats = new ClassStats();
            stats.name = name;
            repo.put( name, stats );
        }
        stats.numObjects++;
        stats.totalSize += size;
    }

    public void describeTo( PrintStream out )
    {
        ArrayList<ClassStats> biggestFirst = new ArrayList<>( repo.values() );
        Collections.sort( biggestFirst, ( o1, o2 ) -> (int) (o2.totalSize - o1.totalSize) );
        out.printf( "accumulated size (b), number of objects, allocation%n" );
        for ( ClassStats stats : biggestFirst )
        {
            out.printf( "%08d,%d,%s%n", stats.totalSize, stats.numObjects, stats.name );
        }
    }
}

class ClassStats {
    String name;
    long numObjects = 0;
    long totalSize = 0;
}
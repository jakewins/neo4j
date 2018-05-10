package org.neo4j.tools.pit;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import static org.neo4j.helpers.collection.MapUtil.map;

public class PointInTimeRecoverTest
{
    @ClassRule
    public static TemporaryFolder dir = new TemporaryFolder();

    private static File root;
    private static File head;

    @BeforeClass
    public static void setup() throws IOException
    {
        // The backup we'll be recovering from
        root = dir.newFolder( "root" );
        createNodes( root, 0, 1 );

        // The latest version of the "production" db - the same
        // as the backup, but with 4 additional transactions..
        head = dir.newFolder( "head" );
        FileUtils.copyRecursively( root, head );
        createNodes( head, 1, 5 );
    }

    @Test
    public void shouldListAvailableRecoveryPoints() throws Exception
    {
        // When
        StdOutCapture stdout = new StdOutCapture();
        try ( AutoCloseable ignore = stdout.capture() )
        {
            PointInTimeRecover.mainReturn( "transactions", root.getAbsolutePath(), head.getAbsolutePath() );
        }

        // Then
        String recording = stdout.recording();
        //    2018-05-10T20:18:31.6Z	4
        //    2018-05-10T20:18:31.6Z	5
        //    2018-05-10T20:18:31.6Z	6
        //    2018-05-10T20:18:31.6Z	7
        assert recording.matches( "(?s)" +
                ".*Z\t4\n" +
                ".*Z\t5\n" +
                ".*Z\t6\n" +
                ".*Z\t7\n" ) :
                String.format( "Expected %s to match assertion", recording );
    }

    @Test
    public void shouldRecoverToPointInTime() throws Exception
    {
        // Given a target we want to move to a point in time
        File target = dir.newFolder();
        FileUtils.copyRecursively( root, target );

        // When I ask to recover the target up to tx 5..
        int exit = PointInTimeRecover.mainReturn( "recover", target.getAbsolutePath(), head.getAbsolutePath(), "5" );

        // Then
        assert exit == 0 : String.format( "Should have exited succesfully, got %d", exit );
        assert lastCommittedTxId( target ) == 5;
    }

    private long lastCommittedTxId( File target )
    {
        long txid;
        GraphDatabaseAPI db = (GraphDatabaseAPI) new GraphDatabaseFactory().newEmbeddedDatabase( target );
        try
        {
            txid = db.getDependencyResolver().resolveDependency(
                    TransactionIdStore.class ).getLastCommittedTransactionId();
        }
        finally
        {
            db.shutdown();
        }
        return txid;
    }

    private static void createNodes( File location, int startIndex, int endIndex )
    {
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( location );
        for ( int i = startIndex; i < endIndex; i++ )
        {
            db.execute( "CREATE (n {index: {index}})", map( "index", i ) ).close();
        }
        db.shutdown();
    }

    class StdOutCapture implements AutoCloseable
    {
        private PrintStream realOut;
        private ByteArrayOutputStream recording;

        AutoCloseable capture()
        {
            realOut = System.out;
            recording = new ByteArrayOutputStream();
            System.setOut( new PrintStream( recording ) );
            return this;
        }

        private String recording() throws UnsupportedEncodingException
        {
            return recording.toString( "UTF-8" );
        }

        @Override
        public void close()
        {
            System.setOut( realOut );
        }
    }
}
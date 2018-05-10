package org.neo4j.tools.pit;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.DefaultFileSystemAbstraction;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.impl.transaction.log.LogEntryCursor;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.impl.transaction.log.entry.LogEntry;
import org.neo4j.kernel.impl.transaction.log.entry.LogEntryCommit;
import org.neo4j.kernel.impl.transaction.log.files.LogFilesBuilder;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import static org.neo4j.tools.util.TransactionLogUtils.openLogs;

public class TransactionsCommand
{
    private final static DateTimeFormatter dateFormat =
            DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss.SX" ).withZone( ZoneOffset.UTC );

    public int execute( String[] argv )
    {
        String backup = argv[1];
        String head = argv[2];

        long startPoint = lastCommittedTx( backup );

        File dir = new File( head );
        try ( FileSystemAbstraction fs = new DefaultFileSystemAbstraction();
                LogEntryCursor logEntryCursor = openLogs( fs,
                        LogFilesBuilder.logFilesBasedOnlyBuilder( dir, fs ).build() ) )
        {
            while ( logEntryCursor.next() )
            {
                LogEntry entry = logEntryCursor.get();
                if ( !(entry instanceof LogEntryCommit) )
                {
                    continue;
                }

                LogEntryCommit commit = (LogEntryCommit) entry;
                if ( commit.getTxId() >= startPoint )
                {
                    System.out.printf( "%s\t%s\n", dateFormat.format( Instant.ofEpochMilli( commit.getTimeWritten() ) ),
                            commit.getTxId() );
                }
            }
            return 0;
        }
        catch ( IOException e )
        {
            System.err.println( e.getMessage() );
            return 1;
        }
    }

    private long lastCommittedTx( String path )
    {
        GraphDatabaseAPI db = (GraphDatabaseAPI) new GraphDatabaseFactory().newEmbeddedDatabase( new File( path ) );
        try
        {
            return db.getDependencyResolver().resolveDependency(
                    TransactionIdStore.class ).getLastCommittedTransactionId();
        }
        finally
        {
            db.shutdown();
        }
    }
}

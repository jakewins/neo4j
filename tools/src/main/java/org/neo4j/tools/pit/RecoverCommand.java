package org.neo4j.tools.pit;

import java.io.File;
import java.io.IOException;

import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.internal.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.tools.applytx.ApplyTransactionsCommand;

public class RecoverCommand
{
    public int execute( String[] argv )
    {
        String target = argv[1];
        String head = argv[2];
        String upToTx = argv[3];

        GraphDatabaseAPI targetDb =
                (GraphDatabaseAPI) new GraphDatabaseFactory().newEmbeddedDatabase( new File( target ) );

        try
        {
            long fromTx = targetDb.getDependencyResolver().resolveDependency(
                    TransactionIdStore.class ).getLastCommittedTransaction().transactionId();

            ApplyTransactionsCommand applyCommand = new ApplyTransactionsCommand( new File( head ), () -> targetDb );
            applyCommand.applyTransactions( new File( head ), targetDb, fromTx, Long.parseLong( upToTx ), System.out );
        }
        catch ( IOException | TransactionFailureException e )
        {
            System.err.println( e.getMessage() );
            return 1;
        }
        finally
        {
            targetDb.shutdown();
        }
        return 0;
    }
}

package org.neo4j.kernel.impl.proc;

import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;

import org.neo4j.function.ThrowingFunction;
import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.security.URLAccessValidationError;
import org.neo4j.kernel.PropertyTracker;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.proc.Procedure;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.coreapi.CoreAPIAvailabilityGuard;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.query.QueryExecutionEngine;
import org.neo4j.kernel.impl.query.QueryExecutionKernelException;
import org.neo4j.kernel.impl.query.QuerySession;
import org.neo4j.kernel.impl.store.StoreId;

public class ProcedureGDSFactory implements ThrowingFunction<Procedure.Context,GraphDatabaseService,ProcedureException>
{
    private final Config config;
    private final DependencyResolver resolver;
    private final Supplier<StoreId> storeId;
    private final Supplier<QueryExecutionEngine> queryExecutor;
    private final CoreAPIAvailabilityGuard availability;

    public ProcedureGDSFactory( Config config,
                                DependencyResolver resolver,
                                Supplier<StoreId> storeId,
                                Supplier<QueryExecutionEngine> queryExecutor,
                                CoreAPIAvailabilityGuard availability )
    {
        this.config = config;
        this.resolver = resolver;
        this.storeId = storeId;
        this.queryExecutor = queryExecutor;
        this.availability = availability;
    }

    @Override
    public GraphDatabaseService apply( Procedure.Context context ) throws ProcedureException
    {
        KernelTransaction transaction = context.get( ReadOperations.KERNEL_TRANSACTION );
        Statement statement = context.get( ReadOperations.STATEMENT );

        GraphDatabaseFacade facade = new GraphDatabaseFacade();
        facade.init( config, new GraphDatabaseFacade.SPI()
        {
            @Override
            public boolean databaseIsAvailable( long timeout )
            {
                return availability.isAvailable( timeout );
            }

            @Override
            public DependencyResolver resolver()
            {
                return resolver;
            }

            @Override
            public StoreId storeId()
            {
                return storeId.get();
            }

            @Override
            public String storeDir()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String name()
            {
                return "ProcedureGraphDatabaseService";
            }

            @Override
            public KernelTransaction currentTransaction()
            {
                availability.assertDatabaseAvailable();
                return transaction;
            }

            @Override
            public boolean isInOpenTransaction()
            {
                return transaction.isOpen();
            }

            @Override
            public Statement currentStatement()
            {
                return statement;
            }

            @Override
            public Result executeQuery( String query, Map<String,Object> parameters, QuerySession querySession )
            {
                try
                {
                    availability.assertDatabaseAvailable();
                    return queryExecutor.get().executeQuery( query, parameters, querySession );
                }
                catch ( QueryExecutionKernelException e )
                {
                    throw e.asUserException();
                }
            }

            @Override
            public void addNodePropertyTracker( PropertyTracker<Node> tracker )
            {
                // TODO
            }

            @Override
            public void removeNodePropertyTracker( PropertyTracker<Node> tracker )
            {
                // TODO
            }

            @Override
            public void addRelationshipPropertyTracker( PropertyTracker<Relationship> tracker )
            {
                // TODO
            }

            @Override
            public void removeRelationshipPropertyTracker( PropertyTracker<Relationship> tracker )
            {
                // TODO
            }

            @Override
            public void registerKernelEventHandler( KernelEventHandler handler )
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void unregisterKernelEventHandler( KernelEventHandler handler )
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> void registerTransactionEventHandler( TransactionEventHandler<T> handler )
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> void unregisterTransactionEventHandler( TransactionEventHandler<T> handler )
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public URL validateURLAccess( URL url ) throws URLAccessValidationError
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void shutdown()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public KernelTransaction beginTransaction()
            {
                throw new UnsupportedOperationException();
            }

        } );

        return facade;
    }
}
